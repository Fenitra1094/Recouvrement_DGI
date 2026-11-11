// ContribuableRepository.java
package com.dgi.recouvrement.repository;
import com.dgi.recouvrement.entity.Contribuable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface ContribuableRepository extends JpaRepository<Contribuable, Integer> {
    Optional<Contribuable> findByEmail(String email);
    Optional<Contribuable> findByNif(String nif);
    
    // Permet de chercher soit par NIF soit par Email
    Optional<Contribuable> findByEmailOrNif(String email, String nif);
    
    boolean existsByEmail(String email);
    boolean existsByNif(String nif);
}

