package com.dgi.recouvrement.repository;

import com.dgi.recouvrement.entity.TypeNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TypeNotificationRepository extends JpaRepository<TypeNotification, Integer> {
    Optional<TypeNotification> findByCode(String code);
}
