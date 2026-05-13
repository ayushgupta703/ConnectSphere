package com.connectsphere.postservice.service;

import com.connectsphere.postservice.client.FollowClient;
import com.connectsphere.postservice.client.LikeClient;
import com.connectsphere.postservice.config.RabbitMqConfig;
import com.connectsphere.postservice.dto.request.CreatePostRequest;
import com.connectsphere.postservice.dto.request.UpdatePostRequest;
import com.connectsphere.postservice.dto.response.PostResponse;
import com.connectsphere.postservice.entity.Post;
import com.connectsphere.postservice.enums.PostVisibility;
import com.connectsphere.postservice.exception.ResourceNotFoundException;
import com.connectsphere.postservice.exception.UnauthorizedException;
import com.connectsphere.postservice.repository.PostRepository;
import com.connectsphere.postservice.service.impl.PostServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostServiceImpl Unit Tests")
class PostServiceImplTest {

    @Mock private PostRepository postRepository;
    @Mock private FollowClient followClient;
    @Mock private LikeClient likeClient;
    @Mock private UserValidationHelper userValidationHelper;
    @Mock private RabbitTemplate rabbitTemplate;

    @InjectMocks private PostServiceImpl postService;

    private UUID ownerId;
    private UUID otherUserId;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();
    }

    // ── helper ────────────────────────────────────────────────────────────
    private Post buildPost(UUID id, UUID userId, PostVisibility visibility) {
        Post p = new Post();
        p.setId(id);
        p.setUserId(userId);
        p.setContent("Hello world");
        p.setVisibility(visibility);
        p.setLikesCount(0);
        p.setCommentsCount(0);
        p.setDeleted(false);
        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());
        return p;
    }

    // ── createPost ────────────────────────────────────────────────────────

    @Test
    @DisplayName("createPost: saves post and returns PostResponse with correct userId and visibility")
    void createPost_success() {
        CreatePostRequest req = new CreatePostRequest();
        req.setContent("Test content");
        req.setVisibility(PostVisibility.PUBLIC);

        UUID postId = UUID.randomUUID();
        Post saved = buildPost(postId, ownerId, PostVisibility.PUBLIC);
        when(postRepository.save(any(Post.class))).thenReturn(saved);

        PostResponse res = postService.createPost(ownerId, req);

        assertNotNull(res);
        assertEquals(ownerId, res.getUserId());
        assertEquals(PostVisibility.PUBLIC, res.getVisibility());
        verify(postRepository).save(any(Post.class));
    }

    // ── updatePost ────────────────────────────────────────────────────────

    @Test
    @DisplayName("updatePost: updates content and visibility then saves")
    void updatePost_success() {
        UUID postId = UUID.randomUUID();
        Post post = buildPost(postId, ownerId, PostVisibility.PUBLIC);

        UpdatePostRequest req = new UpdatePostRequest();
        req.setContent("Updated content");
        req.setVisibility(PostVisibility.PRIVATE);

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(postRepository.save(post)).thenReturn(post);

        postService.updatePost(postId, ownerId, req);

        assertEquals("Updated content", post.getContent());
        assertEquals(PostVisibility.PRIVATE, post.getVisibility());
    }

    @Test
    @DisplayName("updatePost: throws UnauthorizedException when requester is not the owner")
    void updatePost_notOwner_throws() {
        UUID postId = UUID.randomUUID();
        Post post = buildPost(postId, ownerId, PostVisibility.PUBLIC);
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        assertThrows(UnauthorizedException.class,
                () -> postService.updatePost(postId, otherUserId, new UpdatePostRequest()));
    }

    // ── deletePost ────────────────────────────────────────────────────────

    @Test
    @DisplayName("deletePost: soft-deletes post by setting isDeleted to true")
    void deletePost_success() {
        UUID postId = UUID.randomUUID();
        Post post = buildPost(postId, ownerId, PostVisibility.PUBLIC);
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        postService.deletePost(postId, ownerId);

        assertTrue(post.isDeleted());
        verify(postRepository).save(post);
    }

    @Test
    @DisplayName("deletePost: throws UnauthorizedException when not the owner")
    void deletePost_notOwner_throws() {
        UUID postId = UUID.randomUUID();
        Post post = buildPost(postId, ownerId, PostVisibility.PUBLIC);
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        assertThrows(UnauthorizedException.class,
                () -> postService.deletePost(postId, otherUserId));
    }

    // ── getPostById ───────────────────────────────────────────────────────

    @Test
    @DisplayName("getPostById: returns PUBLIC post to any user")
    void getPostById_publicPost_success() {
        UUID postId = UUID.randomUUID();
        Post post = buildPost(postId, ownerId, PostVisibility.PUBLIC);

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(userValidationHelper.isOwnerDeleted(ownerId)).thenReturn(false);
        when(likeClient.hasReacted(eq(postId), eq(otherUserId), anyString())).thenReturn(false);

        PostResponse res = postService.getPostById(postId, otherUserId, "tok");

        assertEquals(postId, res.getId());
    }

    @Test
    @DisplayName("getPostById: owner can view their own PRIVATE post")
    void getPostById_privatePost_byOwner_success() {
        UUID postId = UUID.randomUUID();
        Post post = buildPost(postId, ownerId, PostVisibility.PRIVATE);

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(userValidationHelper.isOwnerDeleted(ownerId)).thenReturn(false);
        when(likeClient.hasReacted(eq(postId), eq(ownerId), anyString())).thenReturn(false);

        PostResponse res = postService.getPostById(postId, ownerId, "tok");

        assertEquals(postId, res.getId());
    }

    @Test
    @DisplayName("getPostById: throws UnauthorizedException for PRIVATE post accessed by non-owner")
    void getPostById_privatePost_byOther_throws() {
        UUID postId = UUID.randomUUID();
        Post post = buildPost(postId, ownerId, PostVisibility.PRIVATE);

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(userValidationHelper.isOwnerDeleted(ownerId)).thenReturn(false);

        assertThrows(UnauthorizedException.class,
                () -> postService.getPostById(postId, otherUserId, "tok"));
    }

    @Test
    @DisplayName("getPostById: follower can view FOLLOWERS_ONLY post")
    void getPostById_followersOnly_byFollower_success() {
        UUID postId = UUID.randomUUID();
        Post post = buildPost(postId, ownerId, PostVisibility.FOLLOWERS_ONLY);

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(userValidationHelper.isOwnerDeleted(ownerId)).thenReturn(false);
        when(followClient.isFollowing(ownerId, "tok")).thenReturn(Map.of("following", true));
        when(likeClient.hasReacted(eq(postId), eq(otherUserId), anyString())).thenReturn(false);

        PostResponse res = postService.getPostById(postId, otherUserId, "tok");

        assertEquals(postId, res.getId());
    }

    @Test
    @DisplayName("getPostById: throws UnauthorizedException for FOLLOWERS_ONLY post when not following")
    void getPostById_followersOnly_nonFollower_throws() {
        UUID postId = UUID.randomUUID();
        Post post = buildPost(postId, ownerId, PostVisibility.FOLLOWERS_ONLY);

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(userValidationHelper.isOwnerDeleted(ownerId)).thenReturn(false);
        when(followClient.isFollowing(ownerId, "tok")).thenReturn(Map.of("following", false));

        assertThrows(UnauthorizedException.class,
                () -> postService.getPostById(postId, otherUserId, "tok"));
    }

    @Test
    @DisplayName("getPostById: throws ResourceNotFoundException for soft-deleted post")
    void getPostById_softDeleted_throws() {
        UUID postId = UUID.randomUUID();
        Post post = buildPost(postId, ownerId, PostVisibility.PUBLIC);
        post.setDeleted(true);
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        assertThrows(ResourceNotFoundException.class,
                () -> postService.getPostById(postId, otherUserId, "tok"));
    }

    @Test
    @DisplayName("getPostById: throws ResourceNotFoundException when owner is deleted")
    void getPostById_deletedOwner_throws() {
        UUID postId = UUID.randomUUID();
        Post post = buildPost(postId, ownerId, PostVisibility.PUBLIC);
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(userValidationHelper.isOwnerDeleted(ownerId)).thenReturn(true);

        assertThrows(ResourceNotFoundException.class,
                () -> postService.getPostById(postId, otherUserId, "tok"));
    }

    // ── getAllPosts ───────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllPosts: filters PRIVATE posts from non-owners and posts from deleted owners")
    void getAllPosts_filtersCorrectly() {
        UUID publicPostId = UUID.randomUUID();
        UUID deletedOwner = UUID.randomUUID();

        Post publicPost = buildPost(publicPostId, ownerId, PostVisibility.PUBLIC);
        Post privatePost = buildPost(UUID.randomUUID(), otherUserId, PostVisibility.PRIVATE);
        Post deletedOwnerPost = buildPost(UUID.randomUUID(), deletedOwner, PostVisibility.PUBLIC);

        Pageable pageable = PageRequest.of(0, 10);
        when(postRepository.findByIsDeletedFalse(pageable))
                .thenReturn(new PageImpl<>(List.of(publicPost, privatePost, deletedOwnerPost)));
        when(userValidationHelper.getDeletedUserIds(any())).thenReturn(Set.of(deletedOwner));

        Page<PostResponse> result = postService.getAllPosts(ownerId, pageable);

        assertEquals(1, result.getContent().size());
        assertEquals(publicPostId, result.getContent().get(0).getId());
    }

    // ── getPostsByUser ────────────────────────────────────────────────────

    @Test
    @DisplayName("getPostsByUser: returns empty page when owner is deleted")
    void getPostsByUser_deletedOwner_returnsEmpty() {
        when(userValidationHelper.isOwnerDeleted(ownerId)).thenReturn(true);

        Page<PostResponse> result = postService.getPostsByUser(ownerId, PageRequest.of(0, 10));

        assertEquals(0, result.getContent().size());
        verify(postRepository, never()).findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(any(), any());
    }

    @Test
    @DisplayName("getPostsByUser: returns paginated posts for active owner")
    void getPostsByUser_success() {
        UUID postId = UUID.randomUUID();
        Post post = buildPost(postId, ownerId, PostVisibility.PUBLIC);
        Pageable pageable = PageRequest.of(0, 10);

        when(userValidationHelper.isOwnerDeleted(ownerId)).thenReturn(false);
        when(postRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(ownerId, pageable))
                .thenReturn(new PageImpl<>(List.of(post)));

        Page<PostResponse> result = postService.getPostsByUser(ownerId, pageable);

        assertEquals(1, result.getContent().size());
        assertEquals(postId, result.getContent().get(0).getId());
    }

    // ── changeVisibility ──────────────────────────────────────────────────

    @Test
    @DisplayName("changeVisibility: updates post visibility and saves")
    void changeVisibility_success() {
        UUID postId = UUID.randomUUID();
        Post post = buildPost(postId, ownerId, PostVisibility.PUBLIC);
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        postService.changeVisibility(postId, ownerId, PostVisibility.PRIVATE);

        assertEquals(PostVisibility.PRIVATE, post.getVisibility());
        verify(postRepository).save(post);
    }

    // ── counter operations ────────────────────────────────────────────────

    @Test
    @DisplayName("incrementLikes: increases likes count by 1")
    void incrementLikes_success() {
        UUID postId = UUID.randomUUID();
        Post post = buildPost(postId, ownerId, PostVisibility.PUBLIC);
        post.setLikesCount(5);
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        postService.incrementLikes(postId);

        assertEquals(6, post.getLikesCount());
        verify(postRepository).save(post);
    }

    @Test
    @DisplayName("decrementLikes: does not go below zero")
    void decrementLikes_floorAtZero() {
        UUID postId = UUID.randomUUID();
        Post post = buildPost(postId, ownerId, PostVisibility.PUBLIC);
        post.setLikesCount(0);
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        postService.decrementLikes(postId);

        assertEquals(0, post.getLikesCount());
    }

    @Test
    @DisplayName("incrementComments: increases comments count by 1")
    void incrementComments_success() {
        UUID postId = UUID.randomUUID();
        Post post = buildPost(postId, ownerId, PostVisibility.PUBLIC);
        post.setCommentsCount(3);
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        postService.incrementComments(postId);

        assertEquals(4, post.getCommentsCount());
    }

    @Test
    @DisplayName("decrementComments: does not go below zero")
    void decrementComments_floorAtZero() {
        UUID postId = UUID.randomUUID();
        Post post = buildPost(postId, ownerId, PostVisibility.PUBLIC);
        post.setCommentsCount(0);
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        postService.decrementComments(postId);

        assertEquals(0, post.getCommentsCount());
    }

    // ── getFeed ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getFeed: returns posts from followed users and own posts")
    void getFeed_success() {
        UUID followedId = UUID.randomUUID();
        Post own = buildPost(UUID.randomUUID(), ownerId, PostVisibility.PUBLIC);
        Post followed = buildPost(UUID.randomUUID(), followedId, PostVisibility.PUBLIC);

        when(followClient.getFollowing(ownerId, "tok")).thenReturn(List.of(followedId));
        when(postRepository.findFeedPosts(anyList(), eq(PostVisibility.PUBLIC)))
                .thenReturn(List.of(own, followed));
        when(userValidationHelper.getDeletedUserIds(any())).thenReturn(Set.of());
        when(likeClient.hasReacted(any(), any(), any())).thenReturn(false);

        List<PostResponse> feed = postService.getFeed(ownerId, "tok");

        assertEquals(2, feed.size());
    }

    @Test
    @DisplayName("getFeed: falls back to own posts when follow-service is unavailable")
    void getFeed_followServiceDown_fallsBackToOwnPosts() {
        UUID postId = UUID.randomUUID();
        Post own = buildPost(postId, ownerId, PostVisibility.PUBLIC);

        when(followClient.getFollowing(ownerId, "tok")).thenThrow(new RuntimeException("down"));
        when(postRepository.findFeedPosts(anyList(), eq(PostVisibility.PUBLIC))).thenReturn(List.of(own));
        when(userValidationHelper.getDeletedUserIds(any())).thenReturn(Set.of());
        when(likeClient.hasReacted(any(), any(), any())).thenReturn(false);

        List<PostResponse> feed = postService.getFeed(ownerId, "tok");

        assertEquals(1, feed.size());
        assertEquals(ownerId, feed.get(0).getUserId());
    }

    // ── getPostsByIds ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getPostsByIds: filters posts from deleted owners")
    void getPostsByIds_filtersDeletedOwners() {
        UUID postId1 = UUID.randomUUID();
        UUID postId2 = UUID.randomUUID();
        UUID deletedOwner = UUID.randomUUID();

        Post visible = buildPost(postId1, ownerId, PostVisibility.PUBLIC);
        Post gone    = buildPost(postId2, deletedOwner, PostVisibility.PUBLIC);

        when(postRepository.findAllById(List.of(postId1, postId2))).thenReturn(List.of(visible, gone));
        when(userValidationHelper.getDeletedUserIds(any())).thenReturn(Set.of(deletedOwner));

        List<PostResponse> result = postService.getPostsByIds(ownerId, List.of(postId1, postId2));

        assertEquals(1, result.size());
        assertEquals(postId1, result.get(0).getId());
    }

    // ── getPostOwner ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getPostOwner: returns owner UUID for existing post")
    void getPostOwner_success() {
        UUID postId = UUID.randomUUID();
        Post post = buildPost(postId, ownerId, PostVisibility.PUBLIC);
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        UUID owner = postService.getPostOwner(postId);

        assertEquals(ownerId, owner);
    }

    @Test
    @DisplayName("getPostOwner: throws ResourceNotFoundException when post not found")
    void getPostOwner_notFound_throws() {
        UUID postId = UUID.randomUUID();
        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> postService.getPostOwner(postId));
    }
}
