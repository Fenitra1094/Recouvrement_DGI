package com.dgi.recouvrement.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "centres_fiscaux")
public class CentreFiscal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_centre_fiscal")
    private Integer idCentreFiscal;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(length = 50)
    private String region;

    @Column(length = 150)
    private String adresse;

    @Column(name = "est_actif")
    private Boolean estActif = true;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_type_centre", nullable = false)
    private TypeCentre typeCentre;

    public Integer getIdCentreFiscal() { return idCentreFiscal; }
    public void setIdCentreFiscal(Integer id) { this.idCentreFiscal = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public Boolean getEstActif() { return estActif; }
    public void setEstActif(Boolean estActif) { this.estActif = estActif; }

    public TypeCentre getTypeCentre() { return typeCentre; }
    public void setTypeCentre(TypeCentre typeCentre) { this.typeCentre = typeCentre; }
}