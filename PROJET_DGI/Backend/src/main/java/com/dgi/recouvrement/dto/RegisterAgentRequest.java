package com.dgi.recouvrement.dto;

public class RegisterAgentRequest {

    private String nom;
    private String prenom;
    private String email;
    private String motDePasse;
    private String roleCode;   // AGENT ou CHEF_CENTRE
    private String centreCode; // SRE_TANÀ, DGE_01, etc.

    public RegisterAgentRequest() {}

    public RegisterAgentRequest(String nom, String prenom, String email, String motDePasse, String roleCode, String centreCode) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.motDePasse = motDePasse;
        this.roleCode = roleCode;
        this.centreCode = centreCode;
    }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }

    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }

    public String getCentreCode() { return centreCode; }
    public void setCentreCode(String centreCode) { this.centreCode = centreCode; }
}