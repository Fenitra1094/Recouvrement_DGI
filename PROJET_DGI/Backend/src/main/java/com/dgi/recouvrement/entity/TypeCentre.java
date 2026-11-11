package com.dgi.recouvrement.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "type_centre")
public class TypeCentre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_type_centre")
    private Integer idTypeCentre;

    @Column(nullable = false, length = 50)
    private String libelle;

    public Integer getIdTypeCentre() { return idTypeCentre; }
    public void setIdTypeCentre(Integer id) { this.idTypeCentre = id; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }
}