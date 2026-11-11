package com.dgi.recouvrement.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "regime_fiscaux")
public class RegimeFiscal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_regime_fiscaux")
    private Integer idRegimeFiscaux;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 100)
    private String libelle;

    @Column(length = 255)
    private String description;

    public Integer getIdRegimeFiscaux() { return idRegimeFiscaux; }
    public void setIdRegimeFiscaux(Integer id) { this.idRegimeFiscaux = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}