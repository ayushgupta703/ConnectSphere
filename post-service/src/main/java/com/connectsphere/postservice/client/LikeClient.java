package com.connectsphere.postservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "like-service", url = "http://localhost:8082")
public interface LikeClient {

    @GetMapping("/api/v1/reactions/{postId}/has-reacted")
    Boolean hasReacted(
            @PathVariable("postId") UUID postId,
            @RequestParam("userId") UUID userId,
            @RequestHeader("Authorization") String token
    );
}
