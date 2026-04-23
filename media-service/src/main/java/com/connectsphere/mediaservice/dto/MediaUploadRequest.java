package com.connectsphere.mediaservice.dto;

import com.connectsphere.mediaservice.entity.MediaType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MediaUploadRequest {

    @NotNull
    private Long uploaderId;

    @NotNull
    private MediaType mediaType;

    private Long sizeKb;

    private String mimeType;

    private Long linkedPostId;
}