package com.dgi.recouvrement.repository;

import com.dgi.recouvrement.entity.Dette;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetteRepository extends JpaRepository<Dette, Long> {
    List<Dette> findByDeclarationContribuableNif(String nif);
}
