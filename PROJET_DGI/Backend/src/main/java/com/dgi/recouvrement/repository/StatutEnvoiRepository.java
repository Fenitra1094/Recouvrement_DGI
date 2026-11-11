package com.dgi.recouvrement.repository;

import com.dgi.recouvrement.entity.StatutEnvoi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StatutEnvoiRepository extends JpaRepository<StatutEnvoi, Integer> {
    Optional<StatutEnvoi> findByLibelle(String libelle);
}
