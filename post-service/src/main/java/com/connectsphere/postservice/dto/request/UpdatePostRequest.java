package com.connectsphere.postservice.dto.request;

import com.connectsphere.postservice.enums.PostVisibility;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UpdatePostRequest {

    @Size(max = 5000, message = "Content cannot exceed 5000 characters")
    private String content;

    private List<String> mediaUrls;

    private PostVisibility visibility;
}