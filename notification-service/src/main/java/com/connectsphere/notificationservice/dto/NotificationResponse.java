package com.connectsphere.notificationservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private String id;
    private String recipientId;
    private String actorId;
    private String type;
    private String message;
    private String targetId;
    private String targetType;
    private boolean isRead;
    private LocalDateTime createdAt;
}