package com.dgi.recouvrement.dto.contribuable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class DashboardSummaryDTO {

    private String nif;
    private String raisonSociale;
    private String regimeFiscal;
    private String centreFiscal;

    // A_JOUR, ECHEANCE_PROCHE, EN_RETARD, EN_ECHEANCIER, EN_ATTENTE_TRAITEMENT
    private String statutGlobal;
    // VERT, ORANGE, ROUGE
    private String couleur;

    private BigDecimal soldePrincipal;
    private BigDecimal soldePenalites;
    private BigDecimal totalPaye;
    private BigDecimal soldeTotal;
    private Integer nombreDeclarationsEnRetard;

    private List<AlerteDTO> alertes;
    private List<EcheanceDTO> echeances;

    public DashboardSummaryDTO() {}

    public String getNif() { return nif; }
    public void setNif(String nif) { this.nif = nif; }
    public String getRaisonSociale() { return raisonSociale; }
    public void setRaisonSociale(String raisonSociale) { this.raisonSociale = raisonSociale; }
    public String getRegimeFiscal() { return regimeFiscal; }
    public void setRegimeFiscal(String regimeFiscal) { this.regimeFiscal = regimeFiscal; }
    public String getCentreFiscal() { return centreFiscal; }
    public void setCentreFiscal(String centreFiscal) { this.centreFiscal = centreFiscal; }
    public String getStatutGlobal() { return statutGlobal; }
    public void setStatutGlobal(String statutGlobal) { this.statutGlobal = statutGlobal; }
    public String getCouleur() { return couleur; }
    public void setCouleur(String couleur) { this.couleur = couleur; }
    public BigDecimal getSoldePrincipal() { return soldePrincipal; }
    public void setSoldePrincipal(BigDecimal soldePrincipal) { this.soldePrincipal = soldePrincipal; }
    public BigDecimal getSoldePenalites() { return soldePenalites; }
    public void setSoldePenalites(BigDecimal soldePenalites) { this.soldePenalites = soldePenalites; }
    public BigDecimal getTotalPaye() { return totalPaye; }
    public void setTotalPaye(BigDecimal totalPaye) { this.totalPaye = totalPaye; }
    public BigDecimal getSoldeTotal() { return soldeTotal; }
    public void setSoldeTotal(BigDecimal soldeTotal) { this.soldeTotal = soldeTotal; }
    public Integer getNombreDeclarationsEnRetard() { return nombreDeclarationsEnRetard; }
    public void setNombreDeclarationsEnRetard(Integer nombreDeclarationsEnRetard) { this.nombreDeclarationsEnRetard = nombreDeclarationsEnRetard; }
    public List<AlerteDTO> getAlertes() { return alertes; }
    public void setAlertes(List<AlerteDTO> alertes) { this.alertes = alertes; }
    public List<EcheanceDTO> getEcheances() { return echeances; }
    public void setEcheances(List<EcheanceDTO> echeances) { this.echeances = echeances; }

    public static class AlerteDTO {
        private String niveau;   // ROUGE, ORANGE, VERT
        private String message;

        public AlerteDTO() {}
        public AlerteDTO(String niveau, String message) {
            this.niveau = niveau;
            this.message = message;
        }

        public String getNiveau() { return niveau; }
        public void setNiveau(String niveau) { this.niveau = niveau; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class EcheanceDTO {
        private Long idDeclaration;
        private String impot;
        private String periode;
        private Integer annee;
        private LocalDate dateLimite;
        private long joursRetard;
        // Montant déclaré à l'origine (avant paiements)
        private BigDecimal montantInitial;
        private BigDecimal montantPrincipal;
        private BigDecimal penalites;
        private BigDecimal montantPaye;
        private BigDecimal resteAPayer;
        // PAYEE, A_PAYER, ECHEANCE_PROCHE, EN_RETARD, EN_ECHEANCIER, EN_ATTENTE_TRAITEMENT
        private String statut;

        public EcheanceDTO() {}

        public Long getIdDeclaration() { return idDeclaration; }
        public void setIdDeclaration(Long idDeclaration) { this.idDeclaration = idDeclaration; }
        public String getImpot() { return impot; }
        public void setImpot(String impot) { this.impot = impot; }
        public String getPeriode() { return periode; }
        public void setPeriode(String periode) { this.periode = periode; }
        public Integer getAnnee() { return annee; }
        public void setAnnee(Integer annee) { this.annee = annee; }
        public LocalDate getDateLimite() { return dateLimite; }
        public void setDateLimite(LocalDate dateLimite) { this.dateLimite = dateLimite; }
        public long getJoursRetard() { return joursRetard; }
        public void setJoursRetard(long joursRetard) { this.joursRetard = joursRetard; }
        public BigDecimal getMontantInitial() { return montantInitial; }
        public void setMontantInitial(BigDecimal montantInitial) { this.montantInitial = montantInitial; }
        public BigDecimal getMontantPrincipal() { return montantPrincipal; }
        public void setMontantPrincipal(BigDecimal montantPrincipal) { this.montantPrincipal = montantPrincipal; }
        public BigDecimal getPenalites() { return penalites; }
        public void setPenalites(BigDecimal penalites) { this.penalites = penalites; }
        public BigDecimal getMontantPaye() { return montantPaye; }
        public void setMontantPaye(BigDecimal montantPaye) { this.montantPaye = montantPaye; }
        public BigDecimal getResteAPayer() { return resteAPayer; }
        public void setResteAPayer(BigDecimal resteAPayer) { this.resteAPayer = resteAPayer; }
        public String getStatut() { return statut; }
        public void setStatut(String statut) { this.statut = statut; }
    }
}
