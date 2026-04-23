package com.connectsphere.postservice.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndexRequestDTO {

    private String postId;
    private String content;
}