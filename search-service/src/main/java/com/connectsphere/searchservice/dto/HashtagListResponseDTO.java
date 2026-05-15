package com.connectsphere.searchservice.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HashtagListResponseDTO {

    private String postId;
    private List<String> hashtags;
}