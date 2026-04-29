package com.connectsphere.mediaservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "follow-service", url = "http://localhost:8084")
public interface FollowClient {

    // 🔥 Get list of users the current user is following
    @GetMapping("/api/v1/follows/following/{userId}")
    List<UUID> getFollowing(@PathVariable UUID userId,
                            @RequestHeader("Authorization") String token);
}