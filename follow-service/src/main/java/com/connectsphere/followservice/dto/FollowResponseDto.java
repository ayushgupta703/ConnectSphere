package com.connectsphere.followservice.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FollowResponseDto {

    private UUID id;
    private UUID followerId;
    private UUID followingId;
    private LocalDateTime createdAt;
}