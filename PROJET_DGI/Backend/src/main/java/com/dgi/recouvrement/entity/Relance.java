package com.dgi.recouvrement.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Relance d'une obligation fiscale (contribuable, impôt, période). Deux natures :
 * - PAIEMENT    : déclarée mais pas réglée ; la dette est constatée et le montant dû est connu ;
 * - DECLARATION : attendue d'après l'assujettissement mais jamais déclarée ; ni dette ni montant.
 * Une campagne regroupe toutes les relances d'un même contribuable dans une seule notification (un seul email).
 */
@Entity
@Table(name = "relance")
public class Relance {

    public static final String PAIEMENT = "PAIEMENT";
    public static final String DECLARATION = "DECLARATION";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_relance")
    private Long id;

    /** PAIEMENT ou DECLARATION */
    @Column(name = "nature", length = 20, nullable = false)
    private String nature;

    // ---- L'obligation relancée (existe même sans déclaration) ----
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_contribuables", nullable = false)
    private Contribuable contribuable;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_types_impots", nullable = false)
    private TypeImpots typeImpots;

    @Column(name = "periode", length = 50, nullable = false)
    private String periode;

    @Column(name = "annee")
    private Integer annee;

    @Column(name = "date_relance", nullable = false)
    private LocalDateTime dateRelance = LocalDateTime.now();

    // Photographie au moment de la relance (le solde évolue ensuite)
    @Column(name = "jours_retard", nullable = false)
    private long joursRetard;

    /** Vide pour une relance DECLARATION : le montant n'est pas connu tant que rien n'est déclaré. */
    @Column(name = "montant_du", precision = 15, scale = 2)
    private BigDecimal montantDu;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_types_relance", nullable = false)
    private TypeRelance typeRelance;

    /** Vide pour une relance DECLARATION (pas de déclaration, donc pas de dette). */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_dettes")
    private Dette dette;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_notification", nullable = false)
    private Notification notification;

    public Relance() {}

    public boolean estNonDeclaree() { return DECLARATION.equals(nature); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNature() { return nature; }
    public void setNature(String nature) { this.nature = nature; }

    public Contribuable getContribuable() { return contribuable; }
    public void setContribuable(Contribuable contribuable) { this.contribuable = contribuable; }

    public TypeImpots getTypeImpots() { return typeImpots; }
    public void setTypeImpots(TypeImpots typeImpots) { this.typeImpots = typeImpots; }

    public String getPeriode() { return periode; }
    public void setPeriode(String periode) { this.periode = periode; }

    public Integer getAnnee() { return annee; }
    public void setAnnee(Integer annee) { this.annee = annee; }

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
