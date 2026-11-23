package com.dgi.recouvrement.repository;

import com.dgi.recouvrement.entity.Dette;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DetteRepository extends JpaRepository<Dette, Long> {
    List<Dette> findByDeclarationContribuableNif(String nif);

    /** Une déclaration a au plus une dette (créée par la relance quand la date limite est dépassée). */
    Optional<Dette> findByDeclarationId(Long declarationId);
}
