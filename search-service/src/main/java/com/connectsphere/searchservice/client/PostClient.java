package com.connectsphere.searchservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "post-service", url = "http://localhost:8081")
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
            @RequestBody List<String> postIds,
            @RequestHeader("Authorization") String token
    );
}