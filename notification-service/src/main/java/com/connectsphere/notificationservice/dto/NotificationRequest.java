package com.connectsphere.notificationservice.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequest {

    private String recipientId; // user who receives notification
    private String actorId;     // user who triggered event
    private String type;        // LIKE, COMMENT, FOLLOW
    private String targetId;    // postId / commentId
}