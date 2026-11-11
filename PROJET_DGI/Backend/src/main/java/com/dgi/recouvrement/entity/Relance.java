package com.dgi.recouvrement.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Relance automatique d'une dette. Une campagne regroupe toutes les dettes relancées
 * d'un même contribuable dans une seule notification (un seul email, pas un par dette).
 */
@Entity
@Table(name = "relance")
public class Relance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_relance")
    private Long id;

    @Column(name = "date_relance", nullable = false)
    private LocalDateTime dateRelance = LocalDateTime.now();

    // Photographie de la dette au moment de la relance (le solde évolue ensuite)
    @Column(name = "jours_retard", nullable = false)
    private long joursRetard;

    @Column(name = "montant_du", precision = 15, scale = 2, nullable = false)
    private BigDecimal montantDu;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_types_relance", nullable = false)
    private TypeRelance typeRelance;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_dettes", nullable = false)
    private Dette dette;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_notification", nullable = false)
    private Notification notification;

    public Relance() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getDateRelance() { return dateRelance; }
    public void setDateRelance(LocalDateTime dateRelance) { this.dateRelance = dateRelance; }

    public long getJoursRetard() { return joursRetard; }
    public void setJoursRetard(long joursRetard) { this.joursRetard = joursRetard; }

    public BigDecimal getMontantDu() { return montantDu; }
    public void setMontantDu(BigDecimal montantDu) { this.montantDu = montantDu; }

    public TypeRelance getTypeRelance() { return typeRelance; }
    public void setTypeRelance(TypeRelance typeRelance) { this.typeRelance = typeRelance; }

    public Dette getDette() { return dette; }
    public void setDette(Dette dette) { this.dette = dette; }

    public Notification getNotification() { return notification; }
    public void setNotification(Notification notification) { this.notification = notification; }
}
