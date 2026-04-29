package com.connectsphere.commentservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "post-service", url = "http://localhost:8081")
public interface PostClient {

    @PostMapping("/api/v1/posts/{id}/comment")
    void incrementComments(@PathVariable("id") UUID postId,
                           @RequestHeader("Authorization") String token);

    @PostMapping("/api/v1/posts/{postId}/uncomment")
    void decrementComments(@PathVariable("postId") UUID postId,
                           @RequestHeader("Authorization") String token);

    @GetMapping("/api/v1/posts/{postId}/owner")
    UUID getPostOwner(@PathVariable("postId") UUID postId,
                      @RequestHeader("Authorization") String token);
}
