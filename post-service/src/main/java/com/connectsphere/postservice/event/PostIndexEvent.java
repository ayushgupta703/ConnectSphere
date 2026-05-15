package com.connectsphere.postservice.event;

import lombok.Data;

@Data
public class PostIndexEvent {
    private String postId;
    private String content;
}
