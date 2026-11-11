package com.dgi.recouvrement.service.dgi;

import com.dgi.recouvrement.config.RelanceProperties;
import com.dgi.recouvrement.entity.Contribuable;
import com.dgi.recouvrement.entity.EnvoiNotification;
import com.dgi.recouvrement.entity.Notification;
import com.dgi.recouvrement.entity.Relance;
import com.dgi.recouvrement.entity.StatutEnvoi;
import com.dgi.recouvrement.repository.EnvoiNotificationRepository;
import com.dgi.recouvrement.repository.RelanceRepository;
import com.dgi.recouvrement.repository.StatutEnvoiRepository;
import com.dgi.recouvrement.service.EmailService;
import com.dgi.recouvrement.service.contribuable.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Expédie les emails de relance mis en file d'attente par la campagne (envoi_notification au statut EN_ATTENTE).
 * Séparé de la campagne : la campagne enregistre vite toutes les relances, puis l'expéditeur envoie
 * par paquets (relance.email.envois-par-passage) sans bloquer la base pendant l'envoi SMTP.
 * Appelé régulièrement par RelanceScheduler ; un seul passage à la fois.
 */
@Service
public class ExpediteurEmails {

    private static final Logger log = LoggerFactory.getLogger(ExpediteurEmails.class);

    private final RelanceProperties props;
    private final EmailService emailService;
    private final ModeleEmailRelance modele;
    private final EnvoiNotificationRepository envoiNotificationRepository;
    private final RelanceRepository relanceRepository;
    private final StatutEnvoiRepository statutEnvoiRepository;

    public ExpediteurEmails(RelanceProperties props, EmailService emailService, ModeleEmailRelance modele,
                            EnvoiNotificationRepository envoiNotificationRepository,
                            RelanceRepository relanceRepository, StatutEnvoiRepository statutEnvoiRepository) {
        this.props = props;
        this.emailService = emailService;
        this.modele = modele;
        this.envoiNotificationRepository = envoiNotificationRepository;
        this.relanceRepository = relanceRepository;
        this.statutEnvoiRepository = statutEnvoiRepository;
    }

    public synchronized void expedier() {
        if (!props.getEmail().isActif()) return;

        List<EnvoiNotification> paquet = envoiNotificationRepository.findByCanalAndStatutEnvoiLibelleOrderByIdAsc(
                "EMAIL", "EN_ATTENTE", PageRequest.of(0, props.getEmail().getEnvoisParPassage()));
        if (paquet.isEmpty()) return;

        // Les relances de toutes les notifications du paquet en une seule requête (contenu de l'email)
        List<Long> idsNotification = paquet.stream().map(e -> e.getNotification().getId()).toList();
        Map<Long, List<Relance>> relancesParNotification = relanceRepository.findByNotificationIdIn(idsNotification).stream()
                .collect(Collectors.groupingBy(r -> r.getNotification().getId()));

        StatutEnvoi envoye = statut("ENVOYE");
        StatutEnvoi echec = statut("ECHEC");
        int envoyes = 0, echecs = 0;

        for (EnvoiNotification envoi : paquet) {
            String erreur = envoyer(envoi, relancesParNotification.getOrDefault(envoi.getNotification().getId(), List.of()));
            envoi.setStatutEnvoi(erreur == null ? envoye : echec);
            envoi.setMessageErreur(erreur != null && erreur.length() > 500 ? erreur.substring(0, 500) : erreur);
            envoi.setDateEnvoi(LocalDateTime.now());
            envoiNotificationRepository.save(envoi);
            if (erreur == null) envoyes++; else echecs++;
        }
        log.info("Expédition des relances : {} email(s) envoyé(s) / {} échec(s)", envoyes, echecs);
    }

    /** Retourne null si l'email est parti, sinon le message d'erreur à enregistrer. */
    private String envoyer(EnvoiNotification envoi, List<Relance> relances) {
        if (!emailService.estConfigure()) {
            return "Compte d'envoi non configuré (MAIL_USERNAME / MAIL_PASSWORD dans le fichier .env)";
        }
        Notification n = envoi.getNotification();
        Contribuable c = n.getContribuable();
        String niveau = relances.stream()
                .map(r -> r.getTypeRelance().getCode())
                .max(Comparator.comparingInt(NotificationService.NIVEAUX::indexOf))
                .orElse("RELANCE_1");
        BigDecimal total = relances.stream().map(Relance::getMontantDu).reduce(BigDecimal.ZERO, BigDecimal::add);
        try {
            emailService.envoyerHtml(envoi.getDestinataire(),
                    modele.sujet(n.getTitre(), c),
                    modele.html(c, niveau, n.getTitre(), n.getMessage(), relances, total,
                            props.getEmail().getLienPlateforme()));
            return null;
        } catch (Exception ex) {
            log.warn("Échec de l'email de relance à {} : {}", envoi.getDestinataire(), ex.getMessage());
            return ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
        }
    }

    private StatutEnvoi statut(String libelle) {
        return statutEnvoiRepository.findByLibelle(libelle)
                .orElseThrow(() -> new IllegalStateException("Statut d'envoi " + libelle + " absent (data.sql)"));
    }
}
