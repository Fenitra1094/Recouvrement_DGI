package com.dgi.recouvrement.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "assujettissement")
public class Assujettissement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_assujettissement")
    private Integer idAssujettissement;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Column(name = "est_actif")
    private Boolean estActif = true;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_types_impots", nullable = false)
    private TypeImpots typeImpots;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_contribuables", nullable = false)
    private Contribuable contribuable;

    // Getters & Setters
    public Integer getIdAssujettissement() { return idAssujettissement; }
    public void setIdAssujettissement(Integer id) { this.idAssujettissement = id; }

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public Boolean getEstActif() { return estActif; }
    public void setEstActif(Boolean estActif) { this.estActif = estActif; }

    public TypeImpots getTypeImpots() { return typeImpots; }
    public void setTypeImpots(TypeImpots typeImpots) { this.typeImpots = typeImpots; }

    public Contribuable getContribuable() { return contribuable; }
    public void setContribuable(Contribuable contribuable) { this.contribuable = contribuable; }
}