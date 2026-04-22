package com.connectsphere.notificationservice.service;

import com.connectsphere.notificationservice.dto.NotificationRequest;
import com.connectsphere.notificationservice.entity.Notification;
import com.connectsphere.notificationservice.entity.NotificationType;
import com.connectsphere.notificationservice.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository repository;

    public NotificationServiceImpl(NotificationRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Notification> getUserNotifications(String userId) {
        return repository.findByRecipientId(userId);
    }

    @Override
    public void markAsRead(String notificationId) {
        Notification notification = repository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        notification.setRead(true);
        repository.save(notification);
    }

    @Override
    public void markAllAsRead(String userId) {
        List<Notification> notifications =
                repository.findByRecipientIdAndIsRead(userId, false);

        notifications.forEach(n -> n.setRead(true));
        repository.saveAll(notifications);
    }

    @Override
    public long getUnreadCount(String userId) {
        return repository.countByRecipientIdAndIsRead(userId, false);
    }

    @Override
    public void createNotification(NotificationRequest request) {

        Notification notification = Notification.builder()
                .recipientId(request.getRecipientId())
                .actorId(request.getActorId())
                .type(NotificationType.valueOf(request.getType()))
                .targetId(request.getTargetId())
                .targetType("POST")
                .message(generateMessage(request.getType()))
                .build();

        repository.save(notification);
    }

    private String generateMessage(String type) {
        return switch (type) {
            case "LIKE" -> "Someone liked your post";
            case "COMMENT" -> "Someone commented on your post";
            case "REPLY" -> "Someone replied to your comment";
            case "FOLLOW" -> "Someone started following you";
            default -> "New notification";
        };
    }
}