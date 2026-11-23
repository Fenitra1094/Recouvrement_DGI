package com.dgi.recouvrement.repository;

import com.dgi.recouvrement.entity.ModePaiement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ModePaiementRepository extends JpaRepository<ModePaiement, Integer> {
    Optional<ModePaiement> findByCode(String code);
}
