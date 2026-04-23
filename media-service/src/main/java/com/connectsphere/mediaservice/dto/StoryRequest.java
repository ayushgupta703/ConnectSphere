package com.connectsphere.mediaservice.dto;

import com.connectsphere.mediaservice.entity.MediaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StoryRequest {

    @NotNull
    private Long authorId;

    @NotBlank
    private String mediaUrl;

    private String caption;

    @NotNull
    private MediaType mediaType;
}