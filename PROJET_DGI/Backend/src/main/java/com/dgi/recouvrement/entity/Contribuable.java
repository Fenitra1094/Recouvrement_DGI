package com.dgi.recouvrement.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "contribuables")
public class Contribuable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_contribuables")
    private Integer idContribuables;

    @Column(nullable = false, unique = true, length = 20)
    private String nif;
    @Column(name = "raison_sociale", nullable = false, length = 150)
    private String raisonSociale;
    @Column(nullable = false, unique = true, length = 100)
    private String email;
    @Column(name = "mot_de_passe", nullable = false)
    private String motDePasse;

    @Column(length = 150)
    private String adresse;
    @Column(length = 30)
    private String telephone;
    @Column(name = "chiffre_affaires_annuel", precision = 15, scale = 2)
    private BigDecimal chiffreAffairesAnnuel = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_statuts_contribuable", nullable = false)
    private StatutContribuable statutContribuable;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_regime_fiscaux", nullable = false)
    private RegimeFiscal regimeFiscal;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_centre_fiscal", nullable = false)
    private CentreFiscal centreFiscal;

    // Getters & Setters
    public Integer getIdContribuables() { return idContribuables; }
    public void setIdContribuables(Integer id) { this.idContribuables = id; }
    public String getNif() { return nif; }
    public void setNif(String nif) { this.nif = nif; }
    public String getRaisonSociale() { return raisonSociale; }
    public void setRaisonSociale(String rs) { this.raisonSociale = rs; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String mdp) { this.motDePasse = mdp; }
    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }
    public String getTelephone() { return telephone; }
    public void setTelephone(String tel) { this.telephone = tel; }
    public BigDecimal getChiffreAffairesAnnuel() { return chiffreAffairesAnnuel; }
    public void setChiffreAffairesAnnuel(BigDecimal ca) { this.chiffreAffairesAnnuel = ca; }
    public StatutContribuable getStatutContribuable() { return statutContribuable; }
    public void setStatutContribuable(StatutContribuable sc) { this.statutContribuable = sc; }
    public RegimeFiscal getRegimeFiscal() { return regimeFiscal; }
    public void setRegimeFiscal(RegimeFiscal rf) { this.regimeFiscal = rf; }
    public CentreFiscal getCentreFiscal() { return centreFiscal; }
    public void setCentreFiscal(CentreFiscal cf) { this.centreFiscal = cf; }
}