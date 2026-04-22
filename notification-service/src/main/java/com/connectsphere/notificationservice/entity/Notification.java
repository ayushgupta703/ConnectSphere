package com.connectsphere.notificationservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    private String id;

    private String recipientId; // who receives notification
    private String actorId;     // who triggered it

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    private String message;

    private String targetId;    // postId / commentId
    private String targetType;  // POST / COMMENT

    private boolean isRead;

    private LocalDateTime createdAt;

    @PrePersist
    public void init() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.isRead = false;
    }
}