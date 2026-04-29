package com.connectsphere.postservice.controller;

import com.connectsphere.postservice.dto.request.CreatePostRequest;
import com.connectsphere.postservice.dto.request.UpdatePostRequest;
import com.connectsphere.postservice.dto.response.PostResponse;
import com.connectsphere.postservice.enums.PostVisibility;
import com.connectsphere.postservice.security.JwtUtil;
import com.connectsphere.postservice.service.PostService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/posts")
public class PostController {

    private final PostService postService;
    private final JwtUtil jwtUtil;

    public PostController(PostService postService, JwtUtil jwtUtil) {
        this.postService = postService;
        this.jwtUtil = jwtUtil;
    }

    // 🔹 Create Post
    @PostMapping
    public ResponseEntity<PostResponse> createPost(
            @RequestAttribute("userId") UUID userId,   // TEMP (later from JWT)
            @Valid @RequestBody CreatePostRequest request
    ) {
        return ResponseEntity.ok(postService.createPost(userId, request));
    }

    // 🔹 Get All Posts (Pagination)
    @GetMapping
    public ResponseEntity<Page<PostResponse>> getAllPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(postService.getAllPosts(PageRequest.of(page, size)));
    }

    // 🔹 Get Posts By User
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<PostResponse>> getPostsByUser(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(postService.getPostsByUser(userId, PageRequest.of(page, size)));
    }

    // 🔹 Update Post
    @PutMapping("/{postId}")
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable UUID postId,
            @RequestAttribute("userId") UUID userId,
            @Valid @RequestBody UpdatePostRequest request
    ) {
        return ResponseEntity.ok(postService.updatePost(postId, userId, request));
    }

    // 🔹 Delete Post (Soft Delete)
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable UUID postId,
            @RequestAttribute("userId") UUID userId
    ) {
        postService.deletePost(postId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> getPostById(
            @PathVariable UUID postId,
            @RequestHeader("Authorization") String token
    ) {
        UUID userId = jwtUtil.extractUserId(token);

        return ResponseEntity.ok(
                postService.getPostById(postId, userId, token)
        );
    }

    @GetMapping("/search")
    public ResponseEntity<Page<PostResponse>> searchPosts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(postService.searchPosts(keyword, PageRequest.of(page, size)));
    }

    @PatchMapping("/{postId}/visibility")
    public ResponseEntity<Void> changeVisibility(
            @PathVariable UUID postId,
            @RequestAttribute("userId") UUID userId,
            @RequestParam String visibility
    ) {
        postService.changeVisibility(postId, userId,
                PostVisibility.valueOf(visibility));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<Void> like(@PathVariable UUID postId) {
        postService.incrementLikes(postId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{postId}/unlike")
    public ResponseEntity<Void> unlike(@PathVariable UUID postId) {
        postService.decrementLikes(postId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{postId}/comment")
    public ResponseEntity<Void> incrementComment(@PathVariable UUID postId) {
        postService.incrementComments(postId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{postId}/uncomment")
    public ResponseEntity<Void> decrementComment(@PathVariable UUID postId) {
        postService.decrementComments(postId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count/{userId}")
    public ResponseEntity<Long> count(@PathVariable UUID userId) {
        return ResponseEntity.ok(postService.getPostCount(userId));
    }

    @GetMapping("/{postId}/owner")
    public ResponseEntity<UUID> getPostOwner(@PathVariable UUID postId) {
        return ResponseEntity.ok(postService.getPostOwner(postId));
    }

    @GetMapping("/feed")
    public ResponseEntity<List<PostResponse>> getFeed(
            @RequestHeader("Authorization") String token) {

        UUID userId = jwtUtil.extractUserId(token);

        return ResponseEntity.ok(postService.getFeed(userId, token));
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<PostResponse>> getPostsByIds(
            @RequestBody List<UUID> postIds
    ) {
        return ResponseEntity.ok(postService.getPostsByIds(postIds));
    }
}