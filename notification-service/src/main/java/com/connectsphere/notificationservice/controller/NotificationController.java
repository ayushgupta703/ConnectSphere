package com.connectsphere.notificationservice.controller;

import com.connectsphere.notificationservice.entity.Notification;
import com.connectsphere.notificationservice.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    // 🔹 Get all notifications for logged-in user
    @GetMapping
    public ResponseEntity<List<Notification>> getUserNotifications(
            @RequestAttribute("userId") UUID userId
    ) {
        return ResponseEntity.ok(service.getUserNotifications(userId.toString()));
    }

    // 🔹 Get unread count
    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(
            @RequestAttribute("userId") UUID userId
    ) {
        return ResponseEntity.ok(service.getUnreadCount(userId.toString()));
    }

    // 🔹 Mark single as read
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable String id) {
        service.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    // 🔹 Mark all as read
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @RequestAttribute("userId") UUID userId
    ) {
        service.markAllAsRead(userId.toString());
        return ResponseEntity.ok().build();
    }
}