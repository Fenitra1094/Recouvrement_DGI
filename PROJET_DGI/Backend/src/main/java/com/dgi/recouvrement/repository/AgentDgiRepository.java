// AgentDgiRepository.java
package com.dgi.recouvrement.repository;
import com.dgi.recouvrement.entity.AgentDgi;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface AgentDgiRepository extends JpaRepository<AgentDgi, Integer> {
    Optional<AgentDgi> findByEmail(String email);
    Optional<AgentDgi> findByMatricule(String matricule);
    Optional<AgentDgi> findByMatriculeOrEmail(String matricule, String email);
}