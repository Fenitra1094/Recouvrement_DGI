package com.dgi.recouvrement.service.dgi;

import com.dgi.recouvrement.config.RelanceProperties;
import com.dgi.recouvrement.dto.NotificationDTO;
import com.dgi.recouvrement.dto.contribuable.DashboardSummaryDTO.EcheanceDTO;
import com.dgi.recouvrement.dto.dgi.JournalRelancesDTO;
import com.dgi.recouvrement.dto.dgi.JournalRelancesDTO.ConfigurationDTO;
import com.dgi.recouvrement.dto.dgi.JournalRelancesDTO.RelanceContribuableDTO;
import com.dgi.recouvrement.dto.dgi.JournalRelancesDTO.StatistiquesDTO;
import com.dgi.recouvrement.dto.dgi.ResultatCampagneDTO;
import com.dgi.recouvrement.entity.*;
import com.dgi.recouvrement.repository.*;
import com.dgi.recouvrement.service.EmailService;
import com.dgi.recouvrement.service.contribuable.CalculEcheance;
import com.dgi.recouvrement.service.contribuable.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Relance automatique des contribuables en retard.
 * - La base trouve les obligations en retard (vue v_obligations_a_relancer) : on ne parcourt pas tous les contribuables.
 * - Les montants sont calculés par CalculEcheance, la même formule que le tableau de bord contribuable,
 *   pour que la relance annonce exactement les montants que le contribuable voit dans son espace.
 * - Le travail est découpé en lots (relance.taille-lot), chacun dans sa propre transaction.
 * - Les emails sont seulement mis en file d'attente ; ExpediteurEmails les envoie ensuite.
 */
@Service
public class RelanceService {

    private static final Logger log = LoggerFactory.getLogger(RelanceService.class);

    private final RelanceProperties props;
    private final CalculEcheance calculEcheance;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final ModeleEmailRelance modele;
    private final TransactionTemplate transaction;
    private final ContribuableRepository contribuableRepository;
    private final DeclarationRepository declarationRepository;
    private final DetteRepository detteRepository;
    private final RelanceRepository relanceRepository;
    private final NotificationRepository notificationRepository;
    private final EnvoiNotificationRepository envoiNotificationRepository;
    private final TypeRelanceRepository typeRelanceRepository;
    private final TypeNotificationRepository typeNotificationRepository;
    private final StatutEnvoiRepository statutEnvoiRepository;

    public RelanceService(RelanceProperties props, CalculEcheance calculEcheance,
                          NotificationService notificationService, EmailService emailService,
                          ModeleEmailRelance modele, PlatformTransactionManager transactionManager,
                          ContribuableRepository contribuableRepository,
                          DeclarationRepository declarationRepository, DetteRepository detteRepository,
                          RelanceRepository relanceRepository, NotificationRepository notificationRepository,
                          EnvoiNotificationRepository envoiNotificationRepository,
                          TypeRelanceRepository typeRelanceRepository,
                          TypeNotificationRepository typeNotificationRepository,
                          StatutEnvoiRepository statutEnvoiRepository) {
        this.props = props;
        this.calculEcheance = calculEcheance;
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.modele = modele;
        this.transaction = new TransactionTemplate(transactionManager);
        this.contribuableRepository = contribuableRepository;
        this.declarationRepository = declarationRepository;
        this.detteRepository = detteRepository;
        this.relanceRepository = relanceRepository;
        this.notificationRepository = notificationRepository;
        this.envoiNotificationRepository = envoiNotificationRepository;
        this.typeRelanceRepository = typeRelanceRepository;
        this.typeNotificationRepository = typeNotificationRepository;
        this.statutEnvoiRepository = statutEnvoiRepository;
    }

    /** Référentiels et compteurs partagés par tous les lots d'une même campagne. */
    private static class Campagne {
        final LocalDateTime maintenant = LocalDateTime.now();
        final LocalDate aujourdHui = maintenant.toLocalDate();
        LocalDateTime limiteAntiSpam;
        TypeNotification typeRelance;
        Map<String, TypeRelance> niveaux;
        StatutEnvoi enAttente;
        int contribuablesRelances, relancesCreees, ignoreesDelai, emailsEnAttente, lotsEnErreur;
    }

    /** Une obligation retenue pour la relance : la ligne de la vue et son calcul. */
    private record Candidate(ObligationARelancer obligation, Declaration declaration, Dette dette, EcheanceDTO echeance) {}

    /**
     * Exécute une campagne de relance :
     * 1. la vue v_obligations_a_relancer donne les contribuables ayant une obligation en retard ;
     * 2. ils sont traités par lots de relance.taille-lot, chaque lot dans sa propre transaction
     *    (une erreur n'annule que son lot) ;
     * 3. pour chaque obligation : calcul du reste à payer, critères de config/relance.properties,
     *    anti-spam, niveau (1re, 2e, mise en demeure) ;
     * 4. une notification par contribuable, regroupant toutes ses dettes relancées ;
     * 5. l'email est mis en file d'attente (EN_ATTENTE), ExpediteurEmails l'envoie ensuite.
     * synchronized : deux clics sur "Lancer maintenant" ne relancent pas deux fois.
     */
    public synchronized ResultatCampagneDTO lancerCampagne(String declencheur) {
        Campagne campagne = new Campagne();
        campagne.limiteAntiSpam = campagne.maintenant.minusDays(props.getDelaiEntreDeuxRelancesJours());
        campagne.typeRelance = typeNotificationRepository.findByCode("RELANCE")
                .orElseThrow(() -> new IllegalStateException("Type de notification RELANCE absent (data.sql)"));
        campagne.niveaux = typeRelanceRepository.findAll().stream()
                .collect(Collectors.toMap(TypeRelance::getCode, Function.identity()));
        campagne.enAttente = statutEnvoiRepository.findByLibelle("EN_ATTENTE")
                .orElseThrow(() -> new IllegalStateException("Statut d'envoi EN_ATTENTE absent (data.sql)"));

        List<Integer> ids = declarationRepository.findIdsContribuablesARelancer();
        for (int debut = 0; debut < ids.size(); debut += props.getTailleLot()) {
            List<Integer> lot = ids.subList(debut, Math.min(debut + props.getTailleLot(), ids.size()));
            try {
                transaction.executeWithoutResult(t -> traiterLot(lot, campagne));
            } catch (RuntimeException ex) {
                campagne.lotsEnErreur++;
                log.error("Relance : lot de {} contribuable(s) annulé (ids {} à {}) : {}",
                        lot.size(), lot.get(0), lot.get(lot.size() - 1), ex.getMessage(), ex);
            }
        }

        log.info("Relance {} : {} contribuable(s), {} relance(s), {} ignorée(s) (délai), {} email(s) en file d'attente{}",
                declencheur, campagne.contribuablesRelances, campagne.relancesCreees, campagne.ignoreesDelai,
                campagne.emailsEnAttente, campagne.lotsEnErreur > 0 ? ", " + campagne.lotsEnErreur + " lot(s) en erreur" : "");
        return new ResultatCampagneDTO(campagne.maintenant, declencheur, campagne.contribuablesRelances,
                campagne.relancesCreees, campagne.ignoreesDelai, campagne.emailsEnAttente, campagne.lotsEnErreur);
    }

    /** Un lot : quelques requêtes groupées (IN ...) au lieu de plusieurs requêtes par contribuable. */
    private void traiterLot(List<Integer> idsContribuables, Campagne campagne) {
        List<ObligationARelancer> obligations =
                declarationRepository.findObligationsARelancer(idsContribuables, campagne.limiteAntiSpam);

        Map<Long, Declaration> declarations = declarationRepository.findAllById(
                        obligations.stream().map(ObligationARelancer::getIdDeclaration).toList()).stream()
                .collect(Collectors.toMap(Declaration::getId, Function.identity()));
        Map<Long, Dette> dettes = detteRepository.findAllById(
                        obligations.stream().map(ObligationARelancer::getIdDette).filter(Objects::nonNull).toList()).stream()
                .collect(Collectors.toMap(Dette::getId, Function.identity()));
        Map<Integer, Contribuable> contribuables = contribuableRepository.findAllById(idsContribuables).stream()
                .collect(Collectors.toMap(Contribuable::getIdContribuables, Function.identity()));

        // Calcul et critères, regroupés par contribuable (la vue est triée par contribuable)
        Map<Integer, List<Candidate>> parContribuable = new LinkedHashMap<>();
        for (ObligationARelancer o : obligations) {
            Declaration declaration = declarations.get(o.getIdDeclaration());
            Dette dette = o.getIdDette() != null ? dettes.get(o.getIdDette()) : null;
            EcheanceDTO e = calculEcheance.calculer(declaration, dette, campagne.aujourdHui);
            if (!meriteRelance(e)) continue;
            if (Boolean.TRUE.equals(o.getRelanceeRecemment())) {
                campagne.ignoreesDelai++;
                continue;
            }
            parContribuable.computeIfAbsent(o.getIdContribuable(), k -> new ArrayList<>())
                    .add(new Candidate(o, declaration, dette, e));
        }

        parContribuable.forEach((idContribuable, candidates) ->
                relancer(contribuables.get(idContribuable), candidates, campagne));
    }

    private void relancer(Contribuable c, List<Candidate> candidates, Campagne campagne) {
        List<Relance> relances = new ArrayList<>();
        for (Candidate cand : candidates) {
            Relance r = new Relance();
            r.setDateRelance(campagne.maintenant);
            r.setDette(cand.dette() != null ? cand.dette() : constaterDette(cand.declaration(), cand.echeance()));
            r.setJoursRetard(cand.echeance().getJoursRetard());
            r.setMontantDu(cand.echeance().getResteAPayer());
            r.setTypeRelance(niveauSuivant(cand.obligation().getNbRelances(), campagne));
            relances.add(r);
        }

        String niveau = relances.stream()
                .map(r -> r.getTypeRelance().getCode())
                .max(Comparator.comparingInt(NotificationService.NIVEAUX::indexOf))
                .orElse("RELANCE_1");
        BigDecimal total = relances.stream().map(Relance::getMontantDu).reduce(BigDecimal.ZERO, BigDecimal::add);

        Notification n = new Notification();
        n.setContribuable(c);
        n.setTypeNotification(campagne.typeRelance);
        n.setDateEnvoi(campagne.maintenant);
        n.setTitre(modele.titre(niveau, relances.size()));
        n.setMessage(modele.message(niveau, relances.size(), total));
        notificationRepository.save(n);

        relances.forEach(r -> r.setNotification(n));
        relanceRepository.saveAll(relances);
        campagne.contribuablesRelances++;
        campagne.relancesCreees += relances.size();

        if (mettreEmailEnAttente(c, n, campagne)) campagne.emailsEnAttente++;
    }

    private boolean meriteRelance(EcheanceDTO e) {
        boolean enRetard = "EN_RETARD".equals(e.getStatut())
                || (props.isInclureEcheanciers() && "EN_ECHEANCIER".equals(e.getStatut()) && e.getJoursRetard() > 0);
        return enRetard
                && e.getJoursRetard() >= props.getJoursRetardMinimum()
                && e.getResteAPayer().signum() > 0
                && e.getResteAPayer().compareTo(props.getMontantMinimum()) >= 0;
    }

    /** Escalade : aucune relance -> 1re ; puis 2e ; à partir du seuil configuré -> mise en demeure. */
    private TypeRelance niveauSuivant(long relancesDejaFaites, Campagne campagne) {
        String code = relancesDejaFaites >= props.getMiseEnDemeureApres() ? "MISE_EN_DEMEURE"
                : relancesDejaFaites == 0 ? "RELANCE_1" : "RELANCE_2";
        TypeRelance type = campagne.niveaux.get(code);
        if (type == null) throw new IllegalStateException("Type de relance " + code + " absent (data.sql)");
        return type;
    }

    /** Une déclaration en retard sans dette enregistrée : la dette est constatée au moment de la relance. */
    private Dette constaterDette(Declaration declaration, EcheanceDTO e) {
        Dette dette = new Dette();
        dette.setDeclaration(declaration);
        dette.setMontantInitial(e.getMontantInitial());
        dette.setMontantPenalites(e.getPenalites());
        dette.setMontantPaye(e.getMontantPaye());
        dette.setSoldeRestant(e.getResteAPayer());
        dette.setStatut("OUVERTE");
        return detteRepository.save(dette);
    }

    /**
     * Met l'email en file d'attente pour l'adresse enregistrée en base (contribuables.email).
     * Retourne false si aucun email n'est prévu (email désactivé ou adresse absente).
     */
    private boolean mettreEmailEnAttente(Contribuable c, Notification n, Campagne campagne) {
        String adresse = c.getEmail();
        if (!props.getEmail().isActif() || adresse == null || adresse.isBlank()) return false;

        EnvoiNotification envoi = new EnvoiNotification();
        envoi.setNotification(n);
        envoi.setCanal("EMAIL");
        envoi.setDestinataire(adresse);
        envoi.setDateEnvoi(campagne.maintenant);
        envoi.setStatutEnvoi(campagne.enAttente);
        envoiNotificationRepository.save(envoi);
        return true;
    }

    // ===================== Journal côté DGI =====================

    public JournalRelancesDTO getJournal() {
        // Les notifications issues d'une relance, de la plus récente à la plus ancienne
        List<Relance> toutes = relanceRepository.findAllByOrderByDateRelanceDesc();
        Map<Long, Notification> notifications = new LinkedHashMap<>();
        toutes.forEach(r -> notifications.putIfAbsent(r.getNotification().getId(), r.getNotification()));

        List<NotificationDTO> dtos = notificationService.versDTO(new ArrayList<>(notifications.values()));
        List<RelanceContribuableDTO> lignes = new ArrayList<>();
        int i = 0;
        for (Notification n : notifications.values()) {
            Contribuable c = n.getContribuable();
            lignes.add(new RelanceContribuableDTO(c.getNif(), c.getRaisonSociale(), c.getEmail(), c.getTelephone(), dtos.get(i++)));
        }

        long emailsEnvoyes = dtos.stream().flatMap(d -> d.envois().stream()).filter(e -> "ENVOYE".equals(e.statut())).count();
        long emailsEchec = dtos.stream().flatMap(d -> d.envois().stream()).filter(e -> "ECHEC".equals(e.statut())).count();
        StatistiquesDTO stats = new StatistiquesDTO(
                (int) lignes.stream().map(RelanceContribuableDTO::nif).distinct().count(),
                toutes.size(),
                (int) toutes.stream().filter(r -> "MISE_EN_DEMEURE".equals(r.getTypeRelance().getCode())).count(),
                (int) emailsEnvoyes,
                (int) emailsEchec,
                (int) dtos.stream().filter(NotificationDTO::estLue).count(),
                dtos.size(),
                toutes.isEmpty() ? null : toutes.get(0).getDateRelance());

        return new JournalRelancesDTO(configuration(), stats, lignes);
    }

    private ConfigurationDTO configuration() {
        LocalDateTime prochaine = null;
        if (props.isActive()) {
            try {
                ZoneId zone = ZoneId.of(props.getFuseauHoraire());
                ZonedDateTime suivante = CronExpression.parse(props.getPlanification()).next(ZonedDateTime.now(zone));
                prochaine = suivante != null ? suivante.toLocalDateTime() : null;
            } catch (RuntimeException ex) {
                log.warn("Expression de planification invalide : {}", props.getPlanification());
            }
        }
        RelanceProperties.Email email = props.getEmail();
        return new ConfigurationDTO(props.isActive(), props.getPlanification(), decrirePlanification(props.getPlanification()),
                prochaine, props.getJoursRetardMinimum(), props.getMontantMinimum(), props.getDelaiEntreDeuxRelancesJours(),
                props.isInclureEcheanciers(), props.getMiseEnDemeureApres(), email.isActif(),
                emailService.estConfigure(), emailService.getCompte());
    }

    private static final Map<String, String> JOURS = Map.ofEntries(
            Map.entry("MON", "lundi"), Map.entry("TUE", "mardi"), Map.entry("WED", "mercredi"),
            Map.entry("THU", "jeudi"), Map.entry("FRI", "vendredi"), Map.entry("SAT", "samedi"),
            Map.entry("SUN", "dimanche"), Map.entry("0", "dimanche"), Map.entry("1", "lundi"),
            Map.entry("2", "mardi"), Map.entry("3", "mercredi"), Map.entry("4", "jeudi"),
            Map.entry("5", "vendredi"), Map.entry("6", "samedi"), Map.entry("7", "dimanche"));

    /** Traduit les expressions cron courantes en français ("Chaque samedi à 08:00"). */
    static String decrirePlanification(String cron) {
        String[] f = cron.trim().split("\\s+");
        if (f.length != 6 || !f[1].matches("\\d+") || !f[2].matches("\\d+")) return "Expression personnalisée : " + cron;
        String heure = " à " + String.format("%02d:%02d", Integer.parseInt(f[2]), Integer.parseInt(f[1]));
        String jourMois = f[3], mois = f[4], jourSemaine = f[5].toUpperCase();
        boolean tousLesJoursDuMois = jourMois.equals("*") || jourMois.equals("?");
        boolean tousLesJoursSemaine = jourSemaine.equals("*") || jourSemaine.equals("?");

        if (tousLesJoursDuMois && !tousLesJoursSemaine && mois.equals("*")) {
            List<String> jours = Arrays.stream(jourSemaine.split(",")).map(j -> JOURS.get(j)).toList();
            if (jours.contains(null)) return "Expression personnalisée : " + cron;
            String liste = jours.size() == 1 ? jours.get(0)
                    : String.join(", ", jours.subList(0, jours.size() - 1)) + " et " + jours.get(jours.size() - 1);
            return "Chaque " + liste + heure;
        }
        if (tousLesJoursDuMois && tousLesJoursSemaine && mois.equals("*")) return "Chaque jour" + heure;
        if (jourMois.matches("\\d+") && tousLesJoursSemaine) {
            if (mois.equals("*")) return "Le " + jourMois + " de chaque mois" + heure;
            if (mois.matches("(\\*|\\d+)/\\d+")) {
                return "Le " + jourMois + ", tous les " + mois.substring(mois.indexOf('/') + 1) + " mois" + heure;
            }
        }
        return "Expression personnalisée : " + cron;
    }
}
