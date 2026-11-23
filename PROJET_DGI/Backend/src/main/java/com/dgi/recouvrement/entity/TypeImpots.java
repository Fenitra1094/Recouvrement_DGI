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

    // ---- Date limite de paiement (MCD types_impots), lue par CalculImpot.dateLimite ----
    /** Jour du mois de la date limite (ramené au dernier jour si le mois est plus court). */
    @Column(name = "echeance_jour")
    private Integer echeanceJour;

    /** MENSUELLE : nombre de mois après le mois déclaré (1 = mois suivant). Vide pour un impôt annuel. */
    @Column(name = "echeance_decalage_mois")
    private Integer echeanceDecalageMois;

    /** ANNUELLE : mois de l'année qui suit l'exercice (5 = mai). Vide pour un impôt mensuel. */
    @Column(name = "echeance_mois")
    private Integer echeanceMois;

    public Integer getEcheanceJour() { return echeanceJour; }
    public void setEcheanceJour(Integer v) { this.echeanceJour = v; }
    public Integer getEcheanceDecalageMois() { return echeanceDecalageMois; }
    public void setEcheanceDecalageMois(Integer v) { this.echeanceDecalageMois = v; }
    public Integer getEcheanceMois() { return echeanceMois; }
    public void setEcheanceMois(Integer v) { this.echeanceMois = v; }

    public Integer getIdTypesImpots() { return idTypesImpots; }
    public void setIdTypesImpots(Integer id) { this.idTypesImpots = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }

    public String getPeriodicite() { return periodicite; }
    public void setPeriodicite(String periodicite) { this.periodicite = periodicite; }
}