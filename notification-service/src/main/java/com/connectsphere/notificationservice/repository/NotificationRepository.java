package com.connectsphere.notificationservice.repository;

import com.connectsphere.notificationservice.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, String> {

    List<Notification> findByRecipientId(String recipientId);

    List<Notification> findByRecipientIdAndIsRead(String recipientId, boolean isRead);

    long countByRecipientIdAndIsRead(String recipientId, boolean isRead);
}