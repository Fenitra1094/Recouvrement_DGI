package com.dgi.recouvrement.entity;
import jakarta.persistence.*;

@Entity
@Table(name = "roles_agent")
public class RoleAgent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_roles_agent")
    private Integer idRolesAgent;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String libelle;

    // Getters & Setters
    public Integer getIdRolesAgent() { return idRolesAgent; }
    public void setIdRolesAgent(Integer id) { this.idRolesAgent = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }
}