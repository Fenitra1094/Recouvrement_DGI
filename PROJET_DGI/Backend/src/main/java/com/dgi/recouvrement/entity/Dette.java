package com.dgi.recouvrement.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "dettes")
public class Dette {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id_dettes")
    private Long id;

    @Column(name = "montant_initial", precision = 15, scale = 2, nullable = false)
    private BigDecimal montantInitial;

    @Column(name = "montant_penalites", precision = 15, scale = 2)
    private BigDecimal montantPenalites = BigDecimal.ZERO;

    @Column(name = "montant_paye", precision = 15, scale = 2)
    private BigDecimal montantPaye = BigDecimal.ZERO;

    @Column(name = "solde_restant", precision = 15, scale = 2, nullable = false)
    private BigDecimal soldeRestant;

    // OUVERTE, EN_ATTENTE_TRAITEMENT (demande d'échéancier déposée), EN_ECHEANCIER, SOLDEE
    @Column(name = "statut", length = 30)
    private String statut = "OUVERTE";

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "Id_declaration", nullable = false, unique = true)
    private Declaration declaration;

    public Dette() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public BigDecimal getMontantInitial() { return montantInitial; }
    public void setMontantInitial(BigDecimal montantInitial) { this.montantInitial = montantInitial; }

    public BigDecimal getMontantPenalites() { return montantPenalites; }
    public void setMontantPenalites(BigDecimal montantPenalites) { this.montantPenalites = montantPenalites; }

    public BigDecimal getMontantPaye() { return montantPaye; }
    public void setMontantPaye(BigDecimal montantPaye) { this.montantPaye = montantPaye; }

    public BigDecimal getSoldeRestant() { return soldeRestant; }
    public void setSoldeRestant(BigDecimal soldeRestant) { this.soldeRestant = soldeRestant; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Declaration getDeclaration() { return declaration; }
    public void setDeclaration(Declaration declaration) { this.declaration = declaration; }
}