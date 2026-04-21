package com.connectsphere.likeservice.dto;

import com.connectsphere.likeservice.entity.ReactionType;
import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class ReactionSummaryResponse {
    private UUID postId;
    private Map<ReactionType, Long> reactions;
    private long totalCount;
}