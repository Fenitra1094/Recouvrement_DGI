package com.dgi.recouvrement.repository;

import com.dgi.recouvrement.entity.Assujettissement;
import com.dgi.recouvrement.entity.Contribuable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AssujettissementRepository extends JpaRepository<Assujettissement, Integer> {
    List<Assujettissement> findByContribuable(Contribuable contribuable);
    List<Assujettissement> findByContribuableAndEstActifTrue(Contribuable contribuable);
}