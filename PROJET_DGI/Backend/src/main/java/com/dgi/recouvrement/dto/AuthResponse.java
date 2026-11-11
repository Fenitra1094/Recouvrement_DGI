package com.dgi.recouvrement.dto;

public class AuthResponse {
    private String message;
    private String nif;
    private String email;
    private String raisonSociale;
    private String role;
    private String regime;
    private String centreFiscal;

    public AuthResponse(String message, String nif, String email, String raisonSociale, String role, String regime, String centreFiscal) {
        this.message = message;
        this.nif = nif;
        this.email = email;
        this.raisonSociale = raisonSociale;
        this.role = role;
        this.regime = regime;
        this.centreFiscal = centreFiscal;
    }

    // Getters
    public String getMessage() { return message; }
    public String getNif() { return nif; }
    public String getEmail() { return email; }
    public String getRaisonSociale() { return raisonSociale; }
    public String getRole() { return role; }
    public String getRegime() { return regime; }
    public String getCentreFiscal() { return centreFiscal; }
}