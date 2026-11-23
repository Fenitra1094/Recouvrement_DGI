package com.dgi.recouvrement.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "paiements")
public class Paiement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_paiements")
    private Long id;

    @Column(name = "montant_payes", precision = 15, scale = 2, nullable = false)
    private BigDecimal montantPayes;

    @Column(name = "date_paiement", nullable = false)
    private LocalDate datePaiement;

    @Column(name = "reference_transaction", length = 50, nullable = false, unique = true)
    private String referenceTransaction;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_modes_paiement", nullable = false)
    private ModePaiement modePaiement;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_statut_paiement", nullable = false)
    private StatutPaiement statutPaiement;

    /** Bordereau encaissé par ce paiement : la déclaration réglée se lit par bordereau.declaration (MCD). */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_bordereaux_versement", nullable = false)
    private BordereauVersement bordereau;

    /** Chèque uniquement. */
    @Column(name = "numero_cheque", length = 30)
    private String numeroCheque;

    @Column(name = "banque", length = 100)
    private String banque;

    /** Paiement bancaire importé : référence de l'opération sur le relevé de la banque. */
    @Column(name = "reference_banque", length = 50)
    private String referenceBanque;

    /** Agent qui a encaissé au guichet, ou qui a importé le relevé bancaire. */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_agents_dgi")
    private AgentDgi agent;

    public Paiement() {}

    public BordereauVersement getBordereau() { return bordereau; }
    public void setBordereau(BordereauVersement bordereau) { this.bordereau = bordereau; }

    public String getNumeroCheque() { return numeroCheque; }
    public void setNumeroCheque(String numeroCheque) { this.numeroCheque = numeroCheque; }

    public String getBanque() { return banque; }
    public void setBanque(String banque) { this.banque = banque; }

    public String getReferenceBanque() { return referenceBanque; }
    public void setReferenceBanque(String referenceBanque) { this.referenceBanque = referenceBanque; }

    public AgentDgi getAgent() { return agent; }
    public void setAgent(AgentDgi agent) { this.agent = agent; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public BigDecimal getMontantPayes() { return montantPayes; }
    public void setMontantPayes(BigDecimal montantPayes) { this.montantPayes = montantPayes; }

    public LocalDate getDatePaiement() { return datePaiement; }
    public void setDatePaiement(LocalDate datePaiement) { this.datePaiement = datePaiement; }

    public String getReferenceTransaction() { return referenceTransaction; }
    public void setReferenceTransaction(String referenceTransaction) { this.referenceTransaction = referenceTransaction; }

    public ModePaiement getModePaiement() { return modePaiement; }
    public void setModePaiement(ModePaiement modePaiement) { this.modePaiement = modePaiement; }

    public StatutPaiement getStatutPaiement() { return statutPaiement; }
    public void setStatutPaiement(StatutPaiement statutPaiement) { this.statutPaiement = statutPaiement; }

}
