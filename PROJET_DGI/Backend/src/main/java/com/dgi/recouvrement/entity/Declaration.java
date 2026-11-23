package com.dgi.recouvrement.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @Column(name = "statut_paiement", length = 50, nullable = false)
    private String statutPaiement = "A_PAYER";

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "Id_types_impots", nullable = false)
    private TypeImpots typeImpots;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "Id_contribuables", nullable = false)
    private Contribuable contribuable;

    /** Base saisie : CA annuel (IS), revenu imposable (IR), CA HT du mois (TVA), salaire du mois (IRSA). */
    @Column(name = "base_imposable", precision = 15, scale = 2)
    private BigDecimal baseImposable;

    /** TVA uniquement : TVA déductible du mois. */
    @Column(name = "tva_deductible", precision = 15, scale = 2)
    private BigDecimal tvaDeductible;

    /** EN_LIGNE (espace contribuable) ou GUICHET (saisie par un agent). */
    @Column(name = "canal", length = 20)
    private String canal;

    /** Agent qui a saisi la déclaration au guichet ; vide pour une déclaration en ligne. */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_agents_dgi")
    private AgentDgi agent;

    public Declaration() {}

    public BigDecimal getBaseImposable() { return baseImposable; }
    public void setBaseImposable(BigDecimal baseImposable) { this.baseImposable = baseImposable; }

    public BigDecimal getTvaDeductible() { return tvaDeductible; }
    public void setTvaDeductible(BigDecimal tvaDeductible) { this.tvaDeductible = tvaDeductible; }

    public String getCanal() { return canal; }
    public void setCanal(String canal) { this.canal = canal; }

    public AgentDgi getAgent() { return agent; }
    public void setAgent(AgentDgi agent) { this.agent = agent; }

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


    public String getStatutPaiement() { return statutPaiement; }
    public void setStatutPaiement(String statutPaiement) { this.statutPaiement = statutPaiement; }


    public TypeImpots getTypeImpots() { return typeImpots; }
    public void setTypeImpots(TypeImpots typeImpots) { this.typeImpots = typeImpots; }

    public Contribuable getContribuable() { return contribuable; }
    public void setContribuable(Contribuable contribuable) { this.contribuable = contribuable; }
}