package com.connectsphere.notificationservice.controller;

import com.connectsphere.notificationservice.dto.NotificationRequest;
import com.connectsphere.notificationservice.entity.Notification;
import com.connectsphere.notificationservice.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    // 🔹 Create notification (called by other services)
    @PostMapping
    public void createNotification(@RequestBody NotificationRequest request) {
        service.createNotification(request);
    }

    private String generateMessage(NotificationRequest request) {
        return switch (request.getType()) {
            case "LIKE" -> "Someone liked your post";
            case "COMMENT" -> "Someone commented on your post";
            case "REPLY" -> "Someone replied to your comment";
            case "FOLLOW" -> "Someone started following you";
            default -> "New notification";
        };
    }

    // 🔹 Get all notifications for logged-in user
    @GetMapping
    public List<Notification> getUserNotifications(Authentication authentication) {
        return service.getUserNotifications(authentication.getName());
    }

    // 🔹 Get unread count
    @GetMapping("/unread-count")
    public long getUnreadCount(Authentication authentication) {
        return service.getUnreadCount(authentication.getName());
    }

    // 🔹 Mark single as read
    @PutMapping("/{id}/read")
    public void markAsRead(@PathVariable String id) {
        service.markAsRead(id);
    }

    // 🔹 Mark all as read
    @PutMapping("/read-all")
    public void markAllAsRead(Authentication authentication) {
        service.markAllAsRead(authentication.getName());
    }
}