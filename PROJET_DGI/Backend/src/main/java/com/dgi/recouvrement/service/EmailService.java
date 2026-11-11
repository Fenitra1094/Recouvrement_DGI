package com.dgi.recouvrement.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/** Envoi d'emails HTML via le compte SMTP configuré dans .env (MAIL_USERNAME / MAIL_PASSWORD). */
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String compte;
    private final String motDePasse;

    public EmailService(JavaMailSender mailSender,
                        @Value("${spring.mail.username:}") String compte,
                        @Value("${spring.mail.password:}") String motDePasse) {
        this.mailSender = mailSender;
        this.compte = compte;
        this.motDePasse = motDePasse;
    }

    public boolean estConfigure() {
        return !compte.isBlank() && !motDePasse.isBlank();
    }

    public String getCompte() {
        return compte;
    }

    /** L'expéditeur est le compte lui-même (MAIL_USERNAME), sans nom d'affichage. */
    public void envoyerHtml(String destinataire, String sujet, String html) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
        helper.setFrom(compte);
        helper.setTo(destinataire);
        helper.setSubject(sujet);
        helper.setText(html, true);
        mailSender.send(message);
    }
}
