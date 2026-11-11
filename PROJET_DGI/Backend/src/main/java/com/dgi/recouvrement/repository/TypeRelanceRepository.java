package com.dgi.recouvrement.repository;

import com.dgi.recouvrement.entity.TypeRelance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TypeRelanceRepository extends JpaRepository<TypeRelance, Integer> {
    Optional<TypeRelance> findByCode(String code);
}
