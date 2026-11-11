// StatutContribuableRepository.java
package com.dgi.recouvrement.repository;
import com.dgi.recouvrement.entity.StatutContribuable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface StatutContribuableRepository extends JpaRepository<StatutContribuable, Integer> {
    Optional<StatutContribuable> findByCode(String code);
}