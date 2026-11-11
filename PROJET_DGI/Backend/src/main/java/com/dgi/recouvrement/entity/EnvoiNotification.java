package com.dgi.recouvrement.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Une ligne par canal utilisé pour une notification (EMAIL, plus tard SMS), chacun avec son statut. */
@Entity
@Table(name = "envoi_notification")
public class EnvoiNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_envoi_notification")
    private Long id;

    @Column(nullable = false, length = 10)
    private String canal; // EMAIL, SMS

    @Column(nullable = false, length = 100)
    private String destinataire; // adresse email ou numéro de téléphone

    @Column(name = "date_envoi", nullable = false)
    private LocalDateTime dateEnvoi = LocalDateTime.now();

    @Column(name = "message_erreur", length = 500)
    private String messageErreur;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_statut_envoi", nullable = false)
    private StatutEnvoi statutEnvoi;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_notification", nullable = false)
    private Notification notification;

    public EnvoiNotification() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCanal() { return canal; }
    public void setCanal(String canal) { this.canal = canal; }

    public String getDestinataire() { return destinataire; }
    public void setDestinataire(String destinataire) { this.destinataire = destinataire; }

    public LocalDateTime getDateEnvoi() { return dateEnvoi; }
    public void setDateEnvoi(LocalDateTime dateEnvoi) { this.dateEnvoi = dateEnvoi; }

    public String getMessageErreur() { return messageErreur; }
    public void setMessageErreur(String messageErreur) { this.messageErreur = messageErreur; }

    public StatutEnvoi getStatutEnvoi() { return statutEnvoi; }
    public void setStatutEnvoi(StatutEnvoi statutEnvoi) { this.statutEnvoi = statutEnvoi; }

    public Notification getNotification() { return notification; }
    public void setNotification(Notification notification) { this.notification = notification; }
}
