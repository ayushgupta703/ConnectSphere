package com.connectsphere.postservice.dto.response;

import com.connectsphere.postservice.enums.PostVisibility;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class PostResponse {

    private UUID id;
    private UUID userId;
    private String content;
    private List<String> mediaUrls;
    private PostVisibility visibility;
    private long likesCount;
    private long commentsCount;
    private boolean likedByCurrentUser;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}