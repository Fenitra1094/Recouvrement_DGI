// RoleAgentRepository.java
package com.dgi.recouvrement.repository;
import com.dgi.recouvrement.entity.RoleAgent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface RoleAgentRepository extends JpaRepository<RoleAgent, Integer> {
    Optional<RoleAgent> findByCode(String code);
}