package com.dgi.recouvrement.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Message affiché dans l'espace contribuable ; la lecture n'est fiable que sur ce canal. */
@Entity
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notification")
    private Long id;

    @Column(nullable = false, length = 200)
    private String titre;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "date_envoi", nullable = false)
    private LocalDateTime dateEnvoi = LocalDateTime.now();

    @Column(name = "est_lue", nullable = false)
    private boolean estLue = false;

    @Column(name = "date_lecture")
    private LocalDateTime dateLecture;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_types_notification", nullable = false)
    private TypeNotification typeNotification;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_contribuables", nullable = false)
    private Contribuable contribuable;

    public Notification() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getDateEnvoi() { return dateEnvoi; }
    public void setDateEnvoi(LocalDateTime dateEnvoi) { this.dateEnvoi = dateEnvoi; }

    public boolean isEstLue() { return estLue; }
    public void setEstLue(boolean estLue) { this.estLue = estLue; }

    public LocalDateTime getDateLecture() { return dateLecture; }
    public void setDateLecture(LocalDateTime dateLecture) { this.dateLecture = dateLecture; }

    public TypeNotification getTypeNotification() { return typeNotification; }
    public void setTypeNotification(TypeNotification typeNotification) { this.typeNotification = typeNotification; }

    public Contribuable getContribuable() { return contribuable; }
    public void setContribuable(Contribuable contribuable) { this.contribuable = contribuable; }
}
