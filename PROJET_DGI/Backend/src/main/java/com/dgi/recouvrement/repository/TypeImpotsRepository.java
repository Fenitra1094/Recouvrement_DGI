// TypeImpotsRepository.java
package com.dgi.recouvrement.repository;
import com.dgi.recouvrement.entity.TypeImpots;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface TypeImpotsRepository extends JpaRepository<TypeImpots, Integer> {
    Optional<TypeImpots> findByCode(String code);
}