package com.connectsphere.postservice.service.impl;

import com.connectsphere.postservice.client.FollowClient;
import com.connectsphere.postservice.client.SearchClient;
import com.connectsphere.postservice.dto.request.CreatePostRequest;
import com.connectsphere.postservice.dto.request.IndexRequestDTO;
import com.connectsphere.postservice.dto.request.UpdatePostRequest;
import com.connectsphere.postservice.dto.response.PostResponse;
import com.connectsphere.postservice.entity.Post;
import com.connectsphere.postservice.enums.PostVisibility;
import com.connectsphere.postservice.exception.ResourceNotFoundException;
import com.connectsphere.postservice.exception.UnauthorizedException;
import com.connectsphere.postservice.repository.PostRepository;
import com.connectsphere.postservice.service.PostService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final FollowClient followClient;
    private final SearchClient searchClient;

    public PostServiceImpl(PostRepository postRepository, FollowClient followClient, SearchClient searchClient) {
        this.postRepository = postRepository;
        this.followClient = followClient;
        this.searchClient = searchClient;
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

        // 🔥 CALL SEARCH SERVICE (IMPORTANT)
        try {
            IndexRequestDTO indexRequestDTO = IndexRequestDTO.builder()
                    .postId(savedPost.getId().toString())
                    .content(savedPost.getContent())
                    .build();

            searchClient.indexPost(indexRequestDTO);
        } catch (Exception ex) {
            // ⚠️ Do NOT fail post creation if search fails
            // Log instead (use logger in real project)
            System.out.println("Search indexing failed: " + ex.getMessage());
        }

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

    @Override
    public PostResponse getPostById(UUID postId, UUID currentUserId, String token) {

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        if (post.isDeleted()) {
            throw new ResourceNotFoundException("Post not found");
        }

        UUID ownerId = post.getUserId();

        // PUBLIC → allow
        if (post.getVisibility() == PostVisibility.PUBLIC) {
            return mapToResponse(post);
        }

        // PRIVATE → only owner
        if (post.getVisibility() == PostVisibility.PRIVATE) {
            if (!ownerId.equals(currentUserId)) {
                throw new UnauthorizedException("Not allowed");
            }
            return mapToResponse(post);
        }

        // FOLLOWERS_ONLY
        if (post.getVisibility() == PostVisibility.FOLLOWERS_ONLY) {

            // Owner always allowed
            if (ownerId.equals(currentUserId)) {
                return mapToResponse(post);
            }

            try {
                Map<String, Boolean> response =
                        followClient.isFollowing(ownerId, token);

                boolean isFollowing = response.getOrDefault("following", false);

                if (!isFollowing) {
                    throw new UnauthorizedException("Not allowed");
                }

            } catch (Exception ex) {
                throw new RuntimeException("Follow service unavailable");
            }
        }

        return mapToResponse(post);
    }

    @Override
    public Page<PostResponse> searchPosts(String keyword, Pageable pageable) {
        return postRepository
                .findByContentContainingIgnoreCaseAndIsDeletedFalse(keyword, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public void changeVisibility(UUID postId, UUID userId, PostVisibility visibility) {

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        if (!post.getUserId().equals(userId)) {
            throw new UnauthorizedException("Not allowed");
        }

        post.setVisibility(visibility);
        postRepository.save(post);
    }

    @Override
    public void incrementLikes(UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        post.setLikesCount(post.getLikesCount() + 1);
        postRepository.save(post);
    }

    @Override
    public void decrementLikes(UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        if (post.getLikesCount() > 0) {
            post.setLikesCount(post.getLikesCount() - 1);
        }

        postRepository.save(post);
    }

    @Override
    public void incrementComments(UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        post.setCommentsCount(post.getCommentsCount() + 1);
        postRepository.save(post);
    }

    @Override
    public void decrementComments(UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        if (post.getCommentsCount() > 0) {
            post.setCommentsCount(post.getCommentsCount() - 1);
        }
        postRepository.save(post);
    }

    @Override
    public long getPostCount(UUID userId) {
        return postRepository.countByUserId(userId);
    }

    @Override
    public Page<PostResponse> getFeedForUser(List<UUID> userIds, Pageable pageable) {
        return postRepository
                .findByUserIdInAndIsDeletedFalseOrderByCreatedAtDesc(userIds, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public String getPostOwner(UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        return post.getUserId().toString();
    }

    @Override
    public List<PostResponse> getFeed(UUID userId) {
        List<UUID> followingIds = followClient.getFollowing(userId);

        followingIds.add(userId);

        List<Post> posts = postRepository.findByAuthorIdInOrderByCreatedAtDesc(followingIds);

        return posts.stream().map(this::mapToResponse).toList();
    }

    @Override
    public List<PostResponse> getPostsByIds(List<UUID> postIds) {
        return postRepository.findAllById(postIds)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }
}