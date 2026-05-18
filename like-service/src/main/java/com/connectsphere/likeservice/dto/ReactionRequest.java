package com.connectsphere.likeservice.dto;

import com.connectsphere.likeservice.entity.ReactionType;
import lombok.Data;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Data
public class ReactionRequest {
    @NotNull
    private UUID postId;

    @NotNull
    private ReactionType reactionType;
}