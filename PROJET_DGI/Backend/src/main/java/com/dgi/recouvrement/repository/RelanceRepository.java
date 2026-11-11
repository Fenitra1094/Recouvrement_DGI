package com.dgi.recouvrement.repository;

import com.dgi.recouvrement.entity.Relance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// Anti-spam et escalade (dernière relance, nombre de relances par dette) : voir la vue v_obligations_a_relancer
@Repository
public interface RelanceRepository extends JpaRepository<Relance, Long> {
    List<Relance> findByNotificationIdIn(List<Long> idsNotification);

    List<Relance> findAllByOrderByDateRelanceDesc();
}
