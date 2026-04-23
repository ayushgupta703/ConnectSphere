package com.connectsphere.commentservice.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequest {

    private String recipientId;
    private String actorId;
    private String type;      // COMMENT / REPLY
    private String targetId;  // postId or commentId
}