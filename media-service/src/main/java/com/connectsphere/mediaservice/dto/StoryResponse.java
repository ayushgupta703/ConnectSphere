package com.connectsphere.mediaservice.dto;

import com.connectsphere.mediaservice.entity.MediaType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class StoryResponse {

    private UUID id;
    private UUID authorId;
    private String mediaUrl;
    private String caption;
    private MediaType mediaType;
    private Long viewsCount;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}