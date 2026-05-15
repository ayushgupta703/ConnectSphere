package com.connectsphere.searchservice.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostIdsResponseDTO {

    private String hashtag;
    private List<String> postIds;
}