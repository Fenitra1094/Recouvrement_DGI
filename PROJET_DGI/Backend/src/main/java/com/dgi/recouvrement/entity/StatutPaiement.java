package com.dgi.recouvrement.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "statut_paiement")
public class StatutPaiement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_statut_paiement")
    private Integer idStatutPaiement;

    @Column(nullable = false, unique = true, length = 50)
    private String libelle; // VALIDE, EN_ATTENTE, REJETE

    public Integer getIdStatutPaiement() { return idStatutPaiement; }
    public void setIdStatutPaiement(Integer id) { this.idStatutPaiement = id; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }
}
