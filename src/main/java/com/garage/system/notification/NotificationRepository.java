package com.garage.system.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findAllByUser_IdOrderByCreatedAtDesc(Long userId);
    long countByUser_IdAndReadAtIsNull(Long userId);
}
