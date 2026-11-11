package com.dgi.recouvrement.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "statut_envoi")
public class StatutEnvoi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_statut_envoi")
    private Integer idStatutEnvoi;

    @Column(nullable = false, unique = true, length = 50)
    private String libelle; // EN_ATTENTE, ENVOYE, DELIVRE, ECHEC

    public Integer getIdStatutEnvoi() { return idStatutEnvoi; }
    public void setIdStatutEnvoi(Integer id) { this.idStatutEnvoi = id; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }
}
