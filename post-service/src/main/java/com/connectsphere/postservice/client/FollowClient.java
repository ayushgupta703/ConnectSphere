package com.connectsphere.postservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;
import java.util.UUID;

@FeignClient(name = "follow-service", url = "http://localhost:8084")
public interface FollowClient {
    @GetMapping("/api/v1/follows/{userId}/is-following")
    Map<String, Boolean> isFollowing(
            @PathVariable UUID userId,
            @RequestHeader("Authorization") String token);
}
