package com.dgi.recouvrement.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "bordereaux_versement")
public class BordereauVersement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference_bv", length = 50, nullable = false, unique = true)
    private String referenceBv; // BV-2026-XXXXXX

    @Column(name = "montant_total", precision = 15, scale = 2, nullable = false)
    private BigDecimal montantTotal;

    @Column(name = "statut", length = 50, nullable = false)
    private String statut = "A_PAYER"; // A_PAYER, PAYE, EN_SOUFFRANCE

    @Column(name = "date_emission", nullable = false)
    private LocalDate dateEmission = LocalDate.now();

    @Column(name = "date_echeance", nullable = false)
    private LocalDate dateEcheance;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "Id_declaration", nullable = false)
    private Declaration declaration;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "Id_contribuables", nullable = false)
    private Contribuable contribuable;

    public BordereauVersement() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReferenceBv() { return referenceBv; }
    public void setReferenceBv(String referenceBv) { this.referenceBv = referenceBv; }

    public BigDecimal getMontantTotal() { return montantTotal; }
    public void setMontantTotal(BigDecimal montantTotal) { this.montantTotal = montantTotal; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public LocalDate getDateEmission() { return dateEmission; }
    public void setDateEmission(LocalDate dateEmission) { this.dateEmission = dateEmission; }

    public LocalDate getDateEcheance() { return dateEcheance; }
    public void setDateEcheance(LocalDate dateEcheance) { this.dateEcheance = dateEcheance; }

    public Declaration getDeclaration() { return declaration; }
    public void setDeclaration(Declaration declaration) { this.declaration = declaration; }

    public Contribuable getContribuable() { return contribuable; }
    public void setContribuable(Contribuable contribuable) { this.contribuable = contribuable; }
}