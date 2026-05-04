package com.connectsphere.postservice.service.impl;

import com.connectsphere.postservice.client.FollowClient;
import com.connectsphere.postservice.client.LikeClient;
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
import com.connectsphere.postservice.service.UserValidationHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final FollowClient followClient;
    private final LikeClient likeClient;
    private final SearchClient searchClient;
    private final UserValidationHelper userValidationHelper;

    // =========================
    // 🔹 Create Post
    // =========================
    @Override
    public PostResponse createPost(UUID userId, CreatePostRequest request) {
        Post post = new Post();
        post.setUserId(userId);
        post.setContent(request.getContent());
        post.setMediaUrls(request.getMediaUrls());
        post.setVisibility(request.getVisibility());

        Post savedPost = postRepository.save(post);
        log.info("Post created: id={} by userId={}", savedPost.getId(), userId);

        try {
            IndexRequestDTO indexRequest = IndexRequestDTO.builder()
                    .postId(savedPost.getId().toString())
                    .content(savedPost.getContent())
                    .build();
            searchClient.indexPost(indexRequest);
        } catch (Exception ex) {
            log.error("Search indexing failed for postId={}: {}", savedPost.getId(), ex.getMessage());
        }

        return mapToResponse(savedPost);
    }

    // =========================
    // 🔹 Get All Posts (paginated)
    // =========================
    @Override
    public Page<PostResponse> getAllPosts(UUID currentUserId, Pageable pageable) {
        Page<Post> postPage = postRepository.findByIsDeletedFalse(pageable);

        // Collect all unique user IDs from this page — ONE batch call to auth-service
        Set<UUID> userIds = postPage.getContent().stream()
                .map(Post::getUserId)
                .collect(Collectors.toSet());

        Set<UUID> deletedUserIds = userValidationHelper.getDeletedUserIds(userIds);

        List<PostResponse> filteredPosts = postPage.getContent().stream()
                .filter(post -> {
                    boolean ownerDeleted = deletedUserIds.contains(post.getUserId());
                    if (ownerDeleted) {
                        log.debug("Skipping post {} in getAllPosts — owner {} is deleted",
                                post.getId(), post.getUserId());
                    }
                    return !ownerDeleted && isVisible(post, currentUserId);
                })
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        // Keep getTotalElements() for consistent pagination behaviour across pages.
        // Deleted-user posts are rare; the slight over-count is acceptable and avoids
        // page-count instability that would result from using filteredPosts.size().
        return new PageImpl<>(filteredPosts, pageable, postPage.getTotalElements());
    }

    // =========================
    // 🔹 Get Posts By User
    // =========================
    @Override
    public Page<PostResponse> getPostsByUser(UUID userId, Pageable pageable) {
        if (userValidationHelper.isOwnerDeleted(userId)) {
            log.info("getPostsByUser: userId={} is deleted, returning empty page", userId);
            return Page.empty();
        }
        return postRepository
                .findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToResponse);
    }

    // =========================
    // 🔹 Update Post
    // =========================
    @Override
    public PostResponse updatePost(UUID postId, UUID userId, UpdatePostRequest request) {
        Post post = getPostIfOwner(postId, userId);

        if (request.getContent() != null) {
            post.setContent(request.getContent());
        }
        if (request.getMediaUrls() != null) {
            post.setMediaUrls(request.getMediaUrls());
        }
        if (request.getVisibility() != null) {
            post.setVisibility(request.getVisibility());
        }

        return mapToResponse(postRepository.save(post));
    }

    // =========================
    // 🔹 Delete Post (Soft Delete)
    // =========================
    @Override
    public void deletePost(UUID postId, UUID userId) {
        Post post = getPostIfOwner(postId, userId);
        post.setDeleted(true);
        postRepository.save(post);
        log.info("Post soft-deleted: postId={} by userId={}", postId, userId);
    }

    // =========================
    // 🔹 Get Post By ID
    // =========================
    @Override
    public PostResponse getPostById(UUID postId, UUID currentUserId, String token) {
        Post post = getPostOrThrow(postId);

        if (post.isDeleted()) {
            throw new ResourceNotFoundException("Post not found");
        }

        UUID ownerId = post.getUserId();

        // Check if the post owner is a deleted user
        if (userValidationHelper.isOwnerDeleted(ownerId)) {
            log.debug("getPostById: post {} rejected — owner {} is deleted", postId, ownerId);
            throw new ResourceNotFoundException("Post not found");
        }

        return switch (post.getVisibility()) {
            case PUBLIC -> mapToResponse(post, currentUserId, token);
            case PRIVATE -> {
                if (!ownerId.equals(currentUserId)) {
                    throw new UnauthorizedException("Not allowed");
                }
                yield mapToResponse(post, currentUserId, token);
            }
            case FOLLOWERS_ONLY -> {
                if (ownerId.equals(currentUserId)) {
                    yield mapToResponse(post, currentUserId, token);
                }
                boolean isFollowing = false;
                try {
                    Map<String, Boolean> response = followClient.isFollowing(ownerId, token);
                    isFollowing = Boolean.TRUE.equals(response.get("following"));
                } catch (Exception ex) {
                    log.error("follow-service unavailable while checking FOLLOWERS_ONLY access " +
                              "for postId={}: {}", postId, ex.getMessage());
                    throw new UnauthorizedException("Not allowed");
                }
                if (!isFollowing) {
                    throw new UnauthorizedException("Not allowed");
                }
                yield mapToResponse(post, currentUserId, token);
            }
        };
    }

    // =========================
    // 🔹 Search Posts
    // =========================
    @Override
    public Page<PostResponse> searchPosts(UUID currentUserId, String keyword, Pageable pageable) {
        Page<Post> postPage = postRepository
                .findByContentContainingIgnoreCaseAndIsDeletedFalse(keyword, pageable);

        // ONE batch call for all user IDs on this search result page
        Set<UUID> userIds = postPage.getContent().stream()
                .map(Post::getUserId)
                .collect(Collectors.toSet());

        Set<UUID> deletedUserIds = userValidationHelper.getDeletedUserIds(userIds);

        List<PostResponse> filtered = postPage.getContent().stream()
                .filter(post -> {
                    boolean ownerDeleted = deletedUserIds.contains(post.getUserId());
                    if (ownerDeleted) {
                        log.debug("Skipping post {} in searchPosts — owner {} is deleted",
                                post.getId(), post.getUserId());
                    }
                    return !ownerDeleted && isVisible(post, currentUserId);
                })
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(filtered, pageable, postPage.getTotalElements());
    }

    // =========================
    // 🔹 Change Visibility
    // =========================
    @Override
    public void changeVisibility(UUID postId, UUID userId, PostVisibility visibility) {
        Post post = getPostIfOwner(postId, userId);
        post.setVisibility(visibility);
        postRepository.save(post);
        log.info("Visibility changed for postId={} to {} by userId={}", postId, visibility, userId);
    }

    // =========================
    // 🔹 Like / Comment Counters
    // =========================
    @Override
    public void incrementLikes(UUID postId) {
        Post post = getPostOrThrow(postId);
        post.setLikesCount(post.getLikesCount() + 1);
        postRepository.save(post);
    }

    @Override
    public void decrementLikes(UUID postId) {
        Post post = getPostOrThrow(postId);
        if (post.getLikesCount() > 0) {
            post.setLikesCount(post.getLikesCount() - 1);
        }
        postRepository.save(post);
    }

    @Override
    public void incrementComments(UUID postId) {
        Post post = getPostOrThrow(postId);
        post.setCommentsCount(post.getCommentsCount() + 1);
        postRepository.save(post);
    }

    @Override
    public void decrementComments(UUID postId) {
        Post post = getPostOrThrow(postId);
        if (post.getCommentsCount() > 0) {
            post.setCommentsCount(post.getCommentsCount() - 1);
        }
        postRepository.save(post);
    }

    @Override
    public long getPostCount(UUID userId) {
        return postRepository.countByUserId(userId);
    }

    // =========================
    // 🔹 Feed For User (paged — used by profile/follow-based views)
    // =========================
    @Override
    public Page<PostResponse> getFeedForUser(UUID currentUserId, List<UUID> userIds, Pageable pageable) {
        Page<Post> postPage = postRepository
                .findByUserIdInAndIsDeletedFalseOrderByCreatedAtDesc(userIds, pageable);

        // ONE batch call to check deleted owners across this entire page
        Set<UUID> uniqueOwnerIds = postPage.getContent().stream()
                .map(Post::getUserId)
                .collect(Collectors.toSet());

        Set<UUID> deletedUserIds = userValidationHelper.getDeletedUserIds(uniqueOwnerIds);

        List<PostResponse> filtered = postPage.getContent().stream()
                .filter(post -> {
                    boolean ownerDeleted = deletedUserIds.contains(post.getUserId());
                    if (ownerDeleted) {
                        log.debug("Skipping post {} in getFeedForUser — owner {} is deleted",
                                post.getId(), post.getUserId());
                    }
                    return !ownerDeleted && isVisible(post, currentUserId);
                })
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(filtered, pageable, postPage.getTotalElements());
    }

    // =========================
    // 🔹 Main Feed (custom — following + own posts)
    // =========================
    @Override
    public List<PostResponse> getFeed(UUID userId, String token) {
        List<UUID> followingIds;
        try {
            followingIds = new ArrayList<>(followClient.getFollowing(userId, token));
        } catch (Exception ex) {
            log.error("follow-service unavailable while building feed for userId={}: {}. " +
                      "Falling back to own posts only.", userId, ex.getMessage());
            followingIds = new ArrayList<>();
        }
        followingIds.add(userId);

        List<Post> posts = postRepository.findFeedPosts(followingIds, PostVisibility.PUBLIC);

        // Collect all unique owner IDs from the feed — ONE batch auth call
        Set<UUID> ownerIds = posts.stream()
                .map(Post::getUserId)
                .collect(Collectors.toSet());

        Set<UUID> deletedUserIds = userValidationHelper.getDeletedUserIds(ownerIds);
        final List<UUID> finalFollowingIds = followingIds;

        return posts.stream()
                .filter(post -> {
                    boolean ownerDeleted = deletedUserIds.contains(post.getUserId());

                    // PRIVATE check
                    boolean isPrivateAndNotOwner =
                            post.getVisibility() == PostVisibility.PRIVATE &&
                                    !post.getUserId().equals(userId);

                    // FOLLOWERS_ONLY check
                    boolean isFollowersOnlyAndNotAllowed =
                            post.getVisibility() == PostVisibility.FOLLOWERS_ONLY &&
                                    !post.getUserId().equals(userId) &&
                                    !finalFollowingIds.contains(post.getUserId());

                    if (ownerDeleted || isPrivateAndNotOwner || isFollowersOnlyAndNotAllowed) {
                        log.debug("Skipping post {} in getFeed — deleted={}, privateBlocked={}, followersBlocked={}",
                                post.getId(),
                                ownerDeleted,
                                isPrivateAndNotOwner,
                                isFollowersOnlyAndNotAllowed);
                    }

                    return !ownerDeleted &&
                            !isPrivateAndNotOwner &&
                            !isFollowersOnlyAndNotAllowed;
                })
                .map(post -> mapToResponse(post, userId, token))
                .collect(Collectors.toList());
    }

    @Override
    public UUID getPostOwner(UUID postId) {
        return getPostOrThrow(postId).getUserId();
    }

    // =========================
    // 🔹 Bulk Post Lookup (internal — used by search-service etc.)
    // =========================
    @Override
    public List<PostResponse> getPostsByIds(UUID currentUserId, List<UUID> postIds) {
        List<Post> posts = postRepository.findAllById(postIds).stream()
                .filter(post -> !post.isDeleted())
                .collect(Collectors.toList());

        // ONE batch call for all unique owner IDs
        Set<UUID> ownerIds = posts.stream()
                .map(Post::getUserId)
                .collect(Collectors.toSet());

        Set<UUID> deletedUserIds = userValidationHelper.getDeletedUserIds(ownerIds);

        return posts.stream()
                .filter(post -> {
                    boolean ownerDeleted = deletedUserIds.contains(post.getUserId());
                    if (ownerDeleted) {
                        log.debug("Skipping post {} in getPostsByIds — owner {} is deleted",
                                post.getId(), post.getUserId());
                    }
                    return !ownerDeleted && isVisible(post, currentUserId);
                })
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // =========================
    // 🔹 Private Helpers
    // =========================

    private Post getPostOrThrow(UUID postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
    }

    private Post getPostIfOwner(UUID postId, UUID userId) {
        Post post = getPostOrThrow(postId);
        if (!post.getUserId().equals(userId)) {
            log.warn("Unauthorized post access attempt: postId={}, requestingUserId={}", postId, userId);
            throw new UnauthorizedException("Not allowed");
        }
        return post;
    }

    private boolean isVisible(Post post, UUID currentUserId) {
        return post.getVisibility() != PostVisibility.PRIVATE ||
               post.getUserId().equals(currentUserId);
    }

    // =========================
    // 🔹 Mappers
    // =========================

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

    private PostResponse mapToResponse(Post post, UUID currentUserId, String token) {
        PostResponse response = mapToResponse(post);
        try {
            Boolean liked = likeClient.hasReacted(post.getId(), currentUserId, token);
            response.setLikedByCurrentUser(Boolean.TRUE.equals(liked));
        } catch (Exception e) {
            log.error("like-service unavailable while checking reaction for postId={}: {}",
                    post.getId(), e.getMessage());
            response.setLikedByCurrentUser(false);
        }
        return response;
    }
}