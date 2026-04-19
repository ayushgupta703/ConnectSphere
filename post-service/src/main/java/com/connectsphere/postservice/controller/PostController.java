package com.connectsphere.postservice.controller;

import com.connectsphere.postservice.dto.request.CreatePostRequest;
import com.connectsphere.postservice.dto.request.UpdatePostRequest;
import com.connectsphere.postservice.dto.response.PostResponse;
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
            @RequestParam UUID userId,   // TEMP (later from JWT)
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
            @RequestParam UUID userId,
            @Valid @RequestBody UpdatePostRequest request
    ) {
        return postService.updatePost(postId, userId, request);
    }

    // 🔹 Delete Post (Soft Delete)
    @DeleteMapping("/{postId}")
    public void deletePost(
            @PathVariable UUID postId,
            @RequestParam UUID userId
    ) {
        postService.deletePost(postId, userId);
    }
}