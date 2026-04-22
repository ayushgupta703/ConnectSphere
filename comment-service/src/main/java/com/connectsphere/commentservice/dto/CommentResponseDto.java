package com.connectsphere.commentservice.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class CommentResponseDto {

    private UUID id;
    private UUID userId;
    private UUID postId;
    private UUID parentCommentId;
    private String content;
    private int likesCount;
    private int repliesCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}