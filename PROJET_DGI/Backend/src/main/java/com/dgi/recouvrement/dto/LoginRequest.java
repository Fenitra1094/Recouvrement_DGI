package com.dgi.recouvrement.dto;

public class LoginRequest {

    private String identifiant;
    private String email;
    private String motDePasse;

    public LoginRequest() {}

    public String getIdentifiant() { return identifiant; }
    public void setIdentifiant(String identifiant) { this.identifiant = identifiant; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }

    public String getIdentifiantEffectif() {
        if (identifiant != null && !identifiant.isBlank()) return identifiant.trim();
        if (email != null && !email.isBlank()) return email.trim();
        return null;
    }
}