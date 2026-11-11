package com.dgi.recouvrement.entity;

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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_declaration", nullable = false)
    private Declaration declaration;

    public Paiement() {}

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

    public Declaration getDeclaration() { return declaration; }
    public void setDeclaration(Declaration declaration) { this.declaration = declaration; }
}
