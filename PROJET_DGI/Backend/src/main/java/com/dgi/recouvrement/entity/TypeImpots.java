package com.dgi.recouvrement.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "types_impots")
public class TypeImpots {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_types_impots")
    private Integer idTypesImpots;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 100)
    private String libelle;

    @Column(nullable = false, length = 20)
    private String periodicite;

    public Integer getIdTypesImpots() { return idTypesImpots; }
    public void setIdTypesImpots(Integer id) { this.idTypesImpots = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }

    public String getPeriodicite() { return periodicite; }
    public void setPeriodicite(String periodicite) { this.periodicite = periodicite; }
}