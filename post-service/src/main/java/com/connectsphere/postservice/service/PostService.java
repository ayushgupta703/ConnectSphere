package com.connectsphere.postservice.service;

import com.connectsphere.postservice.dto.request.CreatePostRequest;
import com.connectsphere.postservice.dto.request.UpdatePostRequest;
import com.connectsphere.postservice.dto.response.PostResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PostService {

    PostResponse createPost(UUID userId, CreatePostRequest request);

    Page<PostResponse> getAllPosts(Pageable pageable);

    Page<PostResponse> getPostsByUser(UUID userId, Pageable pageable);

    PostResponse updatePost(UUID postId, UUID userId, UpdatePostRequest request);

    void deletePost(UUID postId, UUID userId);
}