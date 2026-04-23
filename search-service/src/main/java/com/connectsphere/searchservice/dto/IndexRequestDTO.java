package com.connectsphere.searchservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndexRequestDTO {

    @NotBlank(message = "Post ID cannot be empty")
    private String postId;

    @NotBlank(message = "Content cannot be empty")
    private String content;
}