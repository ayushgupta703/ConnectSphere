package com.connectsphere.commentservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class CommentRequestDto {

    @NotBlank(message = "Content is required")
    private String content;

    private UUID parentCommentId;
}
