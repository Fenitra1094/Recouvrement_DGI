package com.dgi.recouvrement.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "declaration")
public class Declaration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id_declaration")
    private Long id;

    @Column(name = "periode", length = 50, nullable = false)
    private String periode;

    @Column(name = "annee")
    private Integer annee;

    @Column(name = "date_declaration", nullable = false)
    private LocalDate dateDeclaration = LocalDate.now();

    @Column(name = "date_limite_paiement", nullable = false)
    private LocalDate dateLimitePaiement;

    @Column(name = "montant_principal", precision = 15, scale = 2, nullable = false)
    private BigDecimal montantPrincipal = BigDecimal.ZERO;

    @Column(name = "montant_penalites", precision = 15, scale = 2)
    private BigDecimal montantPenalites = BigDecimal.ZERO;

    @Column(name = "statut_paiement", length = 50, nullable = false)
    private String statutPaiement = "A_PAYER";

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "Id_types_impots", nullable = false)
    private TypeImpots typeImpots;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "Id_contribuables", nullable = false)
    private Contribuable contribuable;

    public Declaration() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPeriode() { return periode; }
    public void setPeriode(String periode) { this.periode = periode; }

    public Integer getAnnee() { return annee; }
    public void setAnnee(Integer annee) { this.annee = annee; }

    public LocalDate getDateDeclaration() { return dateDeclaration; }
    public void setDateDeclaration(LocalDate dateDeclaration) { this.dateDeclaration = dateDeclaration; }

    public LocalDate getDateLimitePaiement() { return dateLimitePaiement; }
    public void setDateLimitePaiement(LocalDate dateLimitePaiement) { this.dateLimitePaiement = dateLimitePaiement; }

    public BigDecimal getMontantPrincipal() { return montantPrincipal; }
    public void setMontantPrincipal(BigDecimal montantPrincipal) { this.montantPrincipal = montantPrincipal; }

    public BigDecimal getMontantPenalites() { return montantPenalites; }
    public void setMontantPenalites(BigDecimal montantPenalites) { this.montantPenalites = montantPenalites; }

    public String getStatutPaiement() { return statutPaiement; }
    public void setStatutPaiement(String statutPaiement) { this.statutPaiement = statutPaiement; }


    public TypeImpots getTypeImpots() { return typeImpots; }
    public void setTypeImpots(TypeImpots typeImpots) { this.typeImpots = typeImpots; }

    public Contribuable getContribuable() { return contribuable; }
    public void setContribuable(Contribuable contribuable) { this.contribuable = contribuable; }
}