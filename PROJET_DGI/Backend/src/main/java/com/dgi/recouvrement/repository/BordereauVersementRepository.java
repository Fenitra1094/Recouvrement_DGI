package com.dgi.recouvrement.repository;

import com.dgi.recouvrement.entity.BordereauVersement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BordereauVersementRepository extends JpaRepository<BordereauVersement, Long> {
    Optional<BordereauVersement> findByReferenceBv(String referenceBv);
    Optional<BordereauVersement> findByDeclarationId(Long declarationId);
}