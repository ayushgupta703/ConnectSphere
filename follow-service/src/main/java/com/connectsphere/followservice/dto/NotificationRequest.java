package com.connectsphere.followservice.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequest {

    private String recipientId; // user being followed
    private String actorId;     // follower
    private String type;        // FOLLOW
    private String targetId;    // can be same as recipientId
}