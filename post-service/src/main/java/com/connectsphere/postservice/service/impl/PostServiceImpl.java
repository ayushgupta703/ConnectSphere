package com.connectsphere.postservice.service.impl;

import com.connectsphere.postservice.dto.request.CreatePostRequest;
import com.connectsphere.postservice.dto.request.UpdatePostRequest;
import com.connectsphere.postservice.dto.response.PostResponse;
import com.connectsphere.postservice.entity.Post;
import com.connectsphere.postservice.exception.ResourceNotFoundException;
import com.connectsphere.postservice.exception.UnauthorizedException;
import com.connectsphere.postservice.repository.PostRepository;
import com.connectsphere.postservice.service.PostService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;

    public PostServiceImpl(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    // 🔹 Create Post
    @Override
    public PostResponse createPost(UUID userId, CreatePostRequest request) {

        Post post = new Post();
        post.setUserId(userId);
        post.setContent(request.getContent());
        post.setMediaUrls(request.getMediaUrls());
        post.setVisibility(request.getVisibility());

        Post savedPost = postRepository.save(post);

        return mapToResponse(savedPost);
    }

    // 🔹 Get All Posts
    @Override
    public Page<PostResponse> getAllPosts(Pageable pageable) {
        return postRepository.findByIsDeletedFalse(pageable)
                .map(this::mapToResponse);
    }

    // 🔹 Get Posts By User
    @Override
    public Page<PostResponse> getPostsByUser(UUID userId, Pageable pageable) {
        return postRepository
                .findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToResponse);
    }

    // 🔹 Update Post
    @Override
    public PostResponse updatePost(UUID postId, UUID userId, UpdatePostRequest request) {

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        if (!post.getUserId().equals(userId)) {
            throw new UnauthorizedException("You are not allowed to update this post");
        }

        if (request.getContent() != null) {
            post.setContent(request.getContent());
        }

        if (request.getMediaUrls() != null) {
            post.setMediaUrls(request.getMediaUrls());
        }

        if (request.getVisibility() != null) {
            post.setVisibility(request.getVisibility());
        }

        Post updatedPost = postRepository.save(post);

        return mapToResponse(updatedPost);
    }

    // 🔹 Delete Post (Soft Delete)
    @Override
    public void deletePost(UUID postId, UUID userId) {

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        if (!post.getUserId().equals(userId)) {
            throw new UnauthorizedException("You are not allowed to delete this post");
        }

        post.setDeleted(true);
        postRepository.save(post);
    }

    // 🔹 Mapper (Entity → DTO)
    private PostResponse mapToResponse(Post post) {

        PostResponse response = new PostResponse();
        response.setId(post.getId());
        response.setUserId(post.getUserId());
        response.setContent(post.getContent());
        response.setMediaUrls(post.getMediaUrls());
        response.setVisibility(post.getVisibility());
        response.setLikesCount(post.getLikesCount());
        response.setCommentsCount(post.getCommentsCount());
        response.setCreatedAt(post.getCreatedAt());
        response.setUpdatedAt(post.getUpdatedAt());

        return response;
    }
}