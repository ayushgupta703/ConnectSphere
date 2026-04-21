package com.connectsphere.likeservice.dto;

import com.connectsphere.likeservice.entity.ReactionType;
import lombok.Data;
import org.antlr.v4.runtime.misc.NotNull;

import java.util.UUID;

@Data
public class ReactionRequest {
    @NotNull
    private UUID postId;

    @NotNull
    private ReactionType reactionType;
}