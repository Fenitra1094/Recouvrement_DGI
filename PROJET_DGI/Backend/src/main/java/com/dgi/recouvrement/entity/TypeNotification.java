package com.dgi.recouvrement.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "types_notification")
public class TypeNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_types_notification")
    private Integer idTypesNotification;

    @Column(nullable = false, unique = true, length = 30)
    private String code; // RELANCE, RAPPEL_ECHEANCE, PAIEMENT_VALIDE, INFORMATION

    @Column(nullable = false, length = 100)
    private String libelle;

    public Integer getIdTypesNotification() { return idTypesNotification; }
    public void setIdTypesNotification(Integer id) { this.idTypesNotification = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }
}
