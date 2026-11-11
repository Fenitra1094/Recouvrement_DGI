package com.dgi.recouvrement.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "types_relance")
public class TypeRelance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_types_relance")
    private Integer idTypesRelance;

    @Column(nullable = false, unique = true, length = 30)
    private String code; // RELANCE_1, RELANCE_2, MISE_EN_DEMEURE

    @Column(nullable = false, length = 100)
    private String libelle;

    public Integer getIdTypesRelance() { return idTypesRelance; }
    public void setIdTypesRelance(Integer id) { this.idTypesRelance = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }
}
