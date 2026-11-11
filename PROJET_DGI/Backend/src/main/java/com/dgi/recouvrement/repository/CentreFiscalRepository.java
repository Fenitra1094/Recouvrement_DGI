// CentreFiscalRepository.java
package com.dgi.recouvrement.repository;
import com.dgi.recouvrement.entity.CentreFiscal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface CentreFiscalRepository extends JpaRepository<CentreFiscal, Integer> {
    Optional<CentreFiscal> findByCode(String code);
}