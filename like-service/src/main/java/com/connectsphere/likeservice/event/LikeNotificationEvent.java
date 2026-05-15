package com.connectsphere.likeservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LikeNotificationEvent {
    private String recipientId;
    private String actorId;
    private String type;
    private String targetId;
}
