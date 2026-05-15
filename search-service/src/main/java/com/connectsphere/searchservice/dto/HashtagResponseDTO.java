package com.connectsphere.searchservice.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HashtagResponseDTO {

    private String tag;
    private Long postCount;
}