package com.connectsphere.mediaservice.dto;

import com.connectsphere.mediaservice.entity.MediaType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class StoryResponse {

    private Long id;
    private Long authorId;
    private String mediaUrl;
    private String caption;
    private MediaType mediaType;
    private Long viewsCount;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}