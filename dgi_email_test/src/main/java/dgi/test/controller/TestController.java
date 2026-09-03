package dgi.test.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
public class TestController {

    @Autowired
    private JavaMailSender mailSender;

    @GetMapping("/api/test-relance")
    public String envoyerRelanceTest(@RequestParam(defaultValue = "votre.email@gmail.com") String email) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            
            // Adresse de destination (la vôtre pour le test)
            message.setTo(email);
            
            // Objet du mail officiel
            message.setSubject("DIRECTION GÉNÉRALE DES IMPÔTS - AVIS DE RELANCE N° 2026-001");
            
            // Contenu du message
            String dateJour = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            String corpsMessage = String.format("""
                DIRECTION GÉNÉRALE DES IMPÔTS
                Service du Système d'Information Fiscale (SSIF)
                -------------------------------------------------
                
                Madame / Monsieur,
                
                Sauf erreur de notre part, nous constatons que la dette fiscale suivante demeure impayée à ce jour (%s) :
                
                • Référence NIF : 1234567890
                • Type d'Impôt  : TVA (Taxe sur la Valeur Ajoutée)
                • Période       : Mai 2026
                • Montant Dû    : 1 500 000 MGA
                
                Nous vous prions de bien vouloir procéder au règlement de cette somme dans les plus brefs délais afin d'éviter toute procédure de recouvrement forcé.
                
                Cordialement,
                Le Service du Recouvrement - DGI Madagascar
                -------------------------------------------------
                Ceci est un envoi automatique généré par le système de recouvrement.
                """, dateJour);

            message.setText(corpsMessage);
            
            // Envoi effectif
            mailSender.send(message);

            return "✅ Email de relance DGI envoyé avec succès à : " + email;

        } catch (Exception e) {
            return "❌ Échec de l'envoi : " + e.getMessage();
        }
    }
}