package com.dgi.recouvrement.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "statuts_contribuable")
public class StatutContribuable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_statuts_contribuable")
    private Integer idStatutsContribuable;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 50)
    private String libelle;

    public Integer getIdStatutsContribuable() { return idStatutsContribuable; }
    public void setIdStatutsContribuable(Integer id) { this.idStatutsContribuable = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }
}