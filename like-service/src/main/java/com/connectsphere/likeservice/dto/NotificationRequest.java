package com.connectsphere.likeservice.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequest {

    private String recipientId; // post owner
    private String actorId;     // who liked
    private String type;        // LIKE
    private String targetId;    // postId
}