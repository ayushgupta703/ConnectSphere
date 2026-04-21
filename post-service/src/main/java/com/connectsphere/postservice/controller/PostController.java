package com.connectsphere.postservice.controller;

import com.connectsphere.postservice.dto.request.CreatePostRequest;
import com.connectsphere.postservice.dto.request.UpdatePostRequest;
import com.connectsphere.postservice.dto.response.PostResponse;
import com.connectsphere.postservice.enums.PostVisibility;
import com.connectsphere.postservice.service.PostService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    // 🔹 Create Post
    @PostMapping
    public PostResponse createPost(
            @RequestAttribute("userId") UUID userId,   // TEMP (later from JWT)
            @Valid @RequestBody CreatePostRequest request
    ) {
        return postService.createPost(userId, request);
    }

    // 🔹 Get All Posts (Pagination)
    @GetMapping
    public Page<PostResponse> getAllPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return postService.getAllPosts(PageRequest.of(page, size));
    }

    // 🔹 Get Posts By User
    @GetMapping("/user/{userId}")
    public Page<PostResponse> getPostsByUser(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return postService.getPostsByUser(userId, PageRequest.of(page, size));
    }

    // 🔹 Update Post
    @PutMapping("/{postId}")
    public PostResponse updatePost(
            @PathVariable UUID postId,
            @RequestAttribute("userId") UUID userId,
            @Valid @RequestBody UpdatePostRequest request
    ) {
        return postService.updatePost(postId, userId, request);
    }

    // 🔹 Delete Post (Soft Delete)
    @DeleteMapping("/{postId}")
    public void deletePost(
            @PathVariable UUID postId,
            @RequestAttribute("userId") UUID userId
    ) {
        postService.deletePost(postId, userId);
    }

    @GetMapping("/{postId}")
    public PostResponse getPostById(@PathVariable UUID postId) {
        return postService.getPostById(postId);
    }

    @GetMapping("/search")
    public Page<PostResponse> searchPosts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return postService.searchPosts(keyword, PageRequest.of(page, size));
    }

    @PatchMapping("/{postId}/visibility")
    public void changeVisibility(
            @PathVariable UUID postId,
            @RequestAttribute("userId") UUID userId,
            @RequestParam String visibility
    ) {
        postService.changeVisibility(postId, userId,
                PostVisibility.valueOf(visibility));
    }

    @PostMapping("/{postId}/like")
    public void like(@PathVariable UUID postId) {
        postService.incrementLikes(postId);
    }

    @PostMapping("/{postId}/unlike")
    public void unlike(@PathVariable UUID postId) {
        postService.decrementLikes(postId);
    }

    @PatchMapping("/{postId}/comment")
    public void comment(@PathVariable UUID postId) {
        postService.incrementComments(postId);
    }

    @GetMapping("/count/{userId}")
    public long count(@PathVariable UUID userId) {
        return postService.getPostCount(userId);
    }
}