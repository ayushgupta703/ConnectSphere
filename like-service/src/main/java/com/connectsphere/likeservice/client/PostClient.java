package com.connectsphere.likeservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(name = "post-service", url = "http://localhost:8081")
public interface PostClient {

    @PostMapping("/api/v1/posts/{id}/like")
    void incrementLikes(@PathVariable("id") UUID postId);

    @PostMapping("/api/v1/posts/{id}/unlike")
    void decrementLikes(@PathVariable("id") UUID postId);

    @GetMapping("/api/v1/posts/{postId}/owner")
    UUID getPostOwner(@PathVariable("id") UUID postId);
}