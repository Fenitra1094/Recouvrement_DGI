package com.dgi.recouvrement.repository;

import com.dgi.recouvrement.entity.Paiement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaiementRepository extends JpaRepository<Paiement, Long> {
    // Paiements d'un contribuable, via la déclaration qu'ils règlent
    List<Paiement> findByDeclarationContribuableNif(String nif);
}
