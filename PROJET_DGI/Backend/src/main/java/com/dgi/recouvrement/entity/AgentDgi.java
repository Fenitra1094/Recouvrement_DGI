package com.dgi.recouvrement.entity;
import jakarta.persistence.*;

@Entity
@Table(name = "agents_dgi")
public class AgentDgi {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_agents_dgi")
    private Integer idAgentsDgi;

    @Column(nullable = false, unique = true, length = 50)
    private String matricule;

    @Column(nullable = false, length = 50)
    private String nom;
    @Column(nullable = false, length = 50)
    private String prenom;
    @Column(nullable = false, unique = true, length = 100)
    private String email;
    @Column(name = "mot_de_passe", nullable = false)
    private String motDePasse;
    @Column(name = "actif", nullable = false)  
    private Boolean actif = true ;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_roles_agent", nullable = false)
    private RoleAgent roleAgent;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_centre_fiscal", nullable = false)
    private CentreFiscal centreFiscal;

    // Getters & Setters
    public Integer getIdAgentsDgi() { return idAgentsDgi; }
    public void setIdAgentsDgi(Integer id) { this.idAgentsDgi = id; }
    public String getMatricule() { return matricule; }
    public void setMatricule(String mat) { this.matricule = mat; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String mdp) { this.motDePasse = mdp; }
    public RoleAgent getRoleAgent() { return roleAgent; }
    public void setRoleAgent(RoleAgent role) { this.roleAgent = role; }
    public CentreFiscal getCentreFiscal() { return centreFiscal; }
    public void setCentreFiscal(CentreFiscal centre) { this.centreFiscal = centre; }
    public Boolean getActif() { return actif; }
    public void setActif(Boolean actif) { this.actif = actif; }
}