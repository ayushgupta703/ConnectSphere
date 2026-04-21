package com.connectsphere.likeservice.service;

import com.connectsphere.likeservice.dto.ReactionRequest;
import com.connectsphere.likeservice.dto.ReactionSummaryResponse;

import java.util.UUID;

public interface LikeService {

    void react(ReactionRequest request, UUID userId);

    void removeReaction(UUID postId, UUID userId);

    boolean hasReacted(UUID postId, UUID userId);

    long getTotalReactions(UUID postId);

    ReactionSummaryResponse getReactionSummary(UUID postId);
}