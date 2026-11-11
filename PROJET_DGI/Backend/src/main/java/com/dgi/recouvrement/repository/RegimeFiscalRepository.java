// RegimeFiscalRepository.java
package com.dgi.recouvrement.repository;
import com.dgi.recouvrement.entity.RegimeFiscal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface RegimeFiscalRepository extends JpaRepository<RegimeFiscal, Integer> {
    Optional<RegimeFiscal> findByCode(String code);
}