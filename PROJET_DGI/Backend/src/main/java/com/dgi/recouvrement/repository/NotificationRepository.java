package com.dgi.recouvrement.repository;

import com.dgi.recouvrement.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByContribuableNifOrderByDateEnvoiDesc(String nif);

    List<Notification> findByContribuableNifAndEstLueFalse(String nif);

    long countByContribuableNifAndEstLueFalse(String nif);
}
