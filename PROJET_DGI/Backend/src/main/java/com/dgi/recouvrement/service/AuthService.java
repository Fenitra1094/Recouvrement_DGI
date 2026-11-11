package com.dgi.recouvrement.service;

import com.dgi.recouvrement.dto.*;
import com.dgi.recouvrement.entity.*;
import com.dgi.recouvrement.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Random;

@Service
public class AuthService {

    @Autowired private ContribuableRepository contribuableRepository;
    @Autowired private AgentDgiRepository agentDgiRepository;
    @Autowired private RoleAgentRepository roleAgentRepository;
    @Autowired private RegimeFiscalRepository regimeFiscalRepository;
    @Autowired private CentreFiscalRepository centreFiscalRepository;
    @Autowired private StatutContribuableRepository statutContribuableRepository;
    @Autowired private TypeImpotsRepository typeImpotsRepository;
    @Autowired private AssujettissementRepository assujettissementRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    // --- CONTRIBUABLE ---
    @Transactional
    public AuthResponse registerContribuable(RegisterRequest req) {
        // 1. Contrôles d'unicité
        if (contribuableRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new RuntimeException("Cet email est déjà utilisé.");
        }
        if (req.getNif() == null || req.getNif().isBlank()) {
            throw new RuntimeException("le NIF est obligatoire.");
        }
        String nif = req.getNif().trim();
        if (!nif.matches("^300\\d{7}$")) {
            throw new RuntimeException("NIF invalide. Format attendu : 300XXXXXXX (10 chiffres).");
        }
        if (contribuableRepository.findByNif(nif).isPresent()) {
            throw new RuntimeException("Ce NIF est déjà rattaché à un compte.");
        }

        // 2. Classification automatique selon le CA (inchangée)
        BigDecimal ca = req.getChiffreAffairesAnnuel() != null
                ? req.getChiffreAffairesAnnuel()
                : BigDecimal.ZERO;

        String codeRegime;
        String codeCentre;

        if (ca.compareTo(new BigDecimal("5000000000")) >= 0) {
            codeRegime = "IS";
            codeCentre = "DGE_01";
        } else if (ca.compareTo(new BigDecimal("100000000")) >= 0) {
            codeRegime = "IR";
            codeCentre = "SRE_TANÀ";
        } else {
            codeRegime = "ISYN";
            codeCentre = "CF_TANÀ_1";
        }

        RegimeFiscal regime = regimeFiscalRepository.findByCode(codeRegime).orElseThrow();
        CentreFiscal centre = centreFiscalRepository.findByCode(codeCentre).orElseThrow();
        StatutContribuable statut = statutContribuableRepository.findByCode("ACTIF").orElseThrow();

        // 3. Création — NIF fourni par le contribuable (plus de Random)
        Contribuable c = new Contribuable();
        c.setNif(nif);
        c.setRaisonSociale(req.getRaisonSociale());
        c.setEmail(req.getEmail());
        c.setMotDePasse(passwordEncoder.encode(req.getMotDePasse()));
        c.setAdresse(req.getAdresse());
        c.setTelephone(req.getTelephone());
        c.setChiffreAffairesAnnuel(ca);
        c.setRegimeFiscal(regime);
        c.setCentreFiscal(centre);
        c.setStatutContribuable(statut);

        c = contribuableRepository.save(c);

        // 4. Assujettissements par défaut
        if ("ISYN".equals(codeRegime)) {
            ajouterImpot(c, "IR");
        } else {
            ajouterImpot(c, "TVA");
            ajouterImpot(c, "IRSA");
            ajouterImpot(c, codeRegime);
        }

        return new AuthResponse(
                "Inscription réussie",
                nif,
                c.getEmail(),
                c.getRaisonSociale(),
                "ROLE_CONTRIBUABLE",
                regime.getLibelle(),
                centre.getNom()
        );
    }
     // --- CONTRIBUABLE LOGIN ---
    // --- CONTRIBUABLE LOGIN ---
    public AuthResponse loginContribuable(LoginRequest req) {
        String identifiant = req.getIdentifiantEffectif();
        if (identifiant == null || identifiant.trim().isEmpty()) {
            throw new RuntimeException("Veuillez saisir votre NIF ou votre Email.");
        }

        if (req.getMotDePasse() == null || req.getMotDePasse().isEmpty()) {
            throw new RuntimeException("Veuillez saisir votre mot de passe.");
        }

        // 1. Recherche par NIF ou par Email
        Contribuable contribuable = contribuableRepository.findByEmailOrNif(identifiant, identifiant)
                .orElseThrow(() -> new RuntimeException("NIF / Email ou mot de passe incorrect."));

        // 2. Vérification du mot de passe BCrypt
        if (!passwordEncoder.matches(req.getMotDePasse(), contribuable.getMotDePasse())) {
            throw new RuntimeException("NIF / Email ou mot de passe incorrect.");
        }

        // 3. RETOURNER AuthResponse (au lieu de l'entité Contribuable JPA !)
        String regimeLibelle = contribuable.getRegimeFiscal() != null ? contribuable.getRegimeFiscal().getLibelle() : "NON_DEFINI";
        String centreNom = contribuable.getCentreFiscal() != null ? contribuable.getCentreFiscal().getNom() : "NON_DEFINI";

        return new AuthResponse(
                "Connexion réussie",
                contribuable.getNif(),
                contribuable.getEmail(),
                contribuable.getRaisonSociale(),
                "ROLE_CONTRIBUABLE",
                regimeLibelle,
                centreNom
        );
    }

    // --- AGENT DGI ---
    public AuthResponse loginAgentDgi(LoginRequest req) {
        // Connexion par email OU par matricule
        AgentDgi agent = agentDgiRepository.findByEmail(req.getEmail())
                .or(() -> agentDgiRepository.findByMatricule(req.getEmail()))
                .orElseThrow(() -> new RuntimeException("Identifiants incorrects."));
        if (!passwordEncoder.matches(req.getMotDePasse(), agent.getMotDePasse())) {
            throw new RuntimeException("Identifiants incorrects.");
        }
        return new AuthResponse("Connexion Agent réussie", agent.getMatricule(), agent.getEmail(), agent.getNom() + " " + agent.getPrenom(), "ROLE_" + agent.getRoleAgent().getCode(), "DGI", agent.getCentreFiscal().getNom());
    }

    //Enregistrement Agent DGI 
    // --- AGENT DGI REGISTER ---
        // --- AGENT DGI REGISTER ---
    @Transactional
    public AuthResponse registerAgentDgi(RegisterAgentRequest req) {
        if (req.getEmail() != null && agentDgiRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new RuntimeException("Cet email est déjà utilisé par un agent.");
        }

        RoleAgent role = roleAgentRepository.findByCode(req.getRoleCode())
                .orElseThrow(() -> new RuntimeException("Rôle introuvable : " + req.getRoleCode()));
        CentreFiscal centre = centreFiscalRepository.findByCode(req.getCentreCode())
                .orElseThrow(() -> new RuntimeException("Centre fiscal introuvable : " + req.getCentreCode()));

        // =========================================================
        // 🔒 GÉNÉRATION SÉQUENTIELLE UNIQUE (SANS DOUBLON POSSIBLE)
        // =========================================================
        int anneeEnCours = LocalDate.now().getYear();
        
        // 1. On compte le nombre d'agents actuellement en base
        long totalAgents = agentDgiRepository.count() + 1;
        
        // 2. On formate le numéro avec des zéros devant (ex: 1 -> "0001", 12 -> "0012")
        String numeroSequentiel = String.format("%04d", totalAgents);
        
        // 3. Format final : AGT-2026-0001
        String matriculeAuto = "AGT-" + anneeEnCours + "-" + numeroSequentiel;
        
        // 4. Double sécurité : si par hasard le matricule existe déjà, on incrémente jusqu'à en trouver un libre
        while (agentDgiRepository.findByMatricule(matriculeAuto).isPresent()) {
            totalAgents++;
            numeroSequentiel = String.format("%04d", totalAgents);
            matriculeAuto = "AGT-" + anneeEnCours + "-" + numeroSequentiel;
        }
        // =========================================================

        AgentDgi agent = new AgentDgi();
        agent.setNom(req.getNom());
        agent.setPrenom(req.getPrenom());
        agent.setEmail(req.getEmail());
        agent.setMatricule(matriculeAuto);
        agent.setMotDePasse(passwordEncoder.encode(req.getMotDePasse()));
        agent.setRoleAgent(role);
        agent.setCentreFiscal(centre);
        agent.setActif(true);

        agent = agentDgiRepository.save(agent);

        return new AuthResponse(
            "Agent créé avec succès", 
            agent.getMatricule(), 
            agent.getEmail(), 
            agent.getNom() + " " + agent.getPrenom(), 
            "ROLE_" + role.getCode(), 
            "DGI", 
            centre.getNom()
        );
    }
    
    private void ajouterImpot(Contribuable c, String code) {
        typeImpotsRepository.findByCode(code).ifPresent(t -> {
            Assujettissement a = new Assujettissement();
            a.setContribuable(c);
            a.setTypeImpots(t);
            a.setDateDebut(LocalDate.now());
            a.setEstActif(true);
            assujettissementRepository.save(a);
        });
    }
}