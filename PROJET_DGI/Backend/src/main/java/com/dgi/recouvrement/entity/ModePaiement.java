package com.dgi.recouvrement.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "modes_paiement")
public class ModePaiement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_modes_paiement")
    private Integer idModesPaiement;

    @Column(nullable = false, unique = true, length = 50)
    private String code; // VIREMENT, MOBILE_MONEY, ESPECES, CHEQUE

    @Column(nullable = false, length = 50)
    private String libelle;

    public Integer getIdModesPaiement() { return idModesPaiement; }
    public void setIdModesPaiement(Integer id) { this.idModesPaiement = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }
}
