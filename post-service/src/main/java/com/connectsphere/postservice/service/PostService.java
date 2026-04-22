package com.connectsphere.postservice.service;

import com.connectsphere.postservice.dto.request.CreatePostRequest;
import com.connectsphere.postservice.dto.request.UpdatePostRequest;
import com.connectsphere.postservice.dto.response.PostResponse;
import com.connectsphere.postservice.enums.PostVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface PostService {

    PostResponse createPost(UUID userId, CreatePostRequest request);

    Page<PostResponse> getAllPosts(Pageable pageable);

    Page<PostResponse> getPostsByUser(UUID userId, Pageable pageable);

    PostResponse getPostById(UUID postId);

    PostResponse updatePost(UUID postId, UUID userId, UpdatePostRequest request);

    void deletePost(UUID postId, UUID userId);

    Page<PostResponse> searchPosts(String keyword, Pageable pageable);

    void changeVisibility(UUID postId, UUID userId, PostVisibility visibility);

    void incrementLikes(UUID postId);

    void decrementLikes(UUID postId);

    void incrementComments(UUID postId);

    void decrementComments(UUID postId);

    long getPostCount(UUID userId);

    Page<PostResponse> getFeedForUser(List<UUID> userIds, Pageable pageable);
}