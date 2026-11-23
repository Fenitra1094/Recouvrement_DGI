package com.dgi.recouvrement.dto;
import java.math.BigDecimal;

public class RegisterRequest {
    private String raisonSociale;
    private String email;
    private String motDePasse;
    private String adresse;
    private String telephone;
    private BigDecimal chiffreAffairesAnnuel;
    private String nif;
    /** CA sous le seuil : option pour l'IR (régime du réel) au lieu de l'Impôt Synthétique. */
    private Boolean optionIr;

    public boolean isOptionIr() { return Boolean.TRUE.equals(optionIr); }
    public void setOptionIr(Boolean optionIr) { this.optionIr = optionIr; }

    //Getters and Setters

    public String getRaisonSociale(){ return raisonSociale; }
    public void setRaisonSociale(String raisonSociale){ this.raisonSociale = raisonSociale; }

    public String getEmail(){ return email; }
    public void setEmail(String email){ this.email = email; }

    public String getMotDePasse(){ return motDePasse; }
    public void setMotDePasse(String motDePasse){ this.motDePasse = motDePasse; }

    public String getAdresse(){ return adresse; }
    public void setAdresse(String adresse){ this.adresse = adresse; }

    public String getTelephone(){ return telephone; }
    public void setTelephone(String telephone){ this.telephone = telephone; }

    // + getters / setters pour nif
    public String getNif() { return nif; }
    public void setNif(String nif) { this.nif = nif; }

    public BigDecimal getChiffreAffairesAnnuel(){ return chiffreAffairesAnnuel; }
    public void setChiffreAffairesAnnuel(BigDecimal chiffreAffairesAnnuel){ this.chiffreAffairesAnnuel = chiffreAffairesAnnuel; }
}