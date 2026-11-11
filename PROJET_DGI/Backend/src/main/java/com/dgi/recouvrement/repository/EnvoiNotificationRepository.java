package com.dgi.recouvrement.repository;

import com.dgi.recouvrement.entity.EnvoiNotification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnvoiNotificationRepository extends JpaRepository<EnvoiNotification, Long> {
    List<EnvoiNotification> findByNotificationIdIn(List<Long> idsNotification);

    /** File d'attente de l'expéditeur : les plus anciens envois EN_ATTENTE d'un canal, par paquet. */
    List<EnvoiNotification> findByCanalAndStatutEnvoiLibelleOrderByIdAsc(String canal, String libelleStatut, Pageable paquet);
}
