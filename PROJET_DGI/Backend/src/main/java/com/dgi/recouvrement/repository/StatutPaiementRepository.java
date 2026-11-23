package com.dgi.recouvrement.repository;

import com.dgi.recouvrement.entity.StatutPaiement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StatutPaiementRepository extends JpaRepository<StatutPaiement, Integer> {
    Optional<StatutPaiement> findByLibelle(String libelle);
}
