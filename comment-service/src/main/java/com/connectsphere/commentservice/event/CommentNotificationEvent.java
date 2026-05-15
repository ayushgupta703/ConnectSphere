package com.connectsphere.commentservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentNotificationEvent {
    private String recipientId;
    private String actorId;
    private String type;
    private String targetId;
}
