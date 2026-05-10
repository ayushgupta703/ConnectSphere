package com.connectsphere.searchservice.client;

import com.connectsphere.searchservice.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "post-service", url = "http://localhost:8081", configuration = FeignConfig.class)
public interface PostClient {

    // ✅ Get single post by ID
    @GetMapping("/api/v1/posts/{postId}")
    Object getPostById(
            @PathVariable("postId") String postId,
            @RequestHeader("Authorization") String token
    );

    // ✅ Get multiple posts (future optimization)
    @PostMapping("/api/v1/posts/bulk")
    List<Object> getPostsByIds(
            @RequestBody List<UUID> postIds
    );
}