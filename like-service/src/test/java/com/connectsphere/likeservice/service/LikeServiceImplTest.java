package com.connectsphere.likeservice.service;

import com.connectsphere.likeservice.client.PostClient;
import com.connectsphere.likeservice.dto.ReactionRequest;
import com.connectsphere.likeservice.dto.ReactionSummaryResponse;
import com.connectsphere.likeservice.entity.Like;
import com.connectsphere.likeservice.entity.ReactionType;
import com.connectsphere.likeservice.exception.ResourceNotFoundException;
import com.connectsphere.likeservice.producer.NotificationEventProducer;
import com.connectsphere.likeservice.repository.LikeRepository;
import com.connectsphere.likeservice.service.impl.LikeServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LikeServiceImpl Unit Tests")
class LikeServiceImplTest {

    @Mock private LikeRepository likeRepository;
    @Mock private PostClient postClient;
    @Mock private NotificationEventProducer notificationEventProducer;

    @InjectMocks private LikeServiceImpl likeService;

    private UUID userId;
    private UUID postId;
    private UUID postOwnerId;

    @BeforeEach
    void setUp() {
        userId      = UUID.randomUUID();
        postId      = UUID.randomUUID();
        postOwnerId = UUID.randomUUID();
    }

    // ── helper ────────────────────────────────────────────────────────────
    private Like buildLike(UUID uid, UUID pid, ReactionType type) {
        return Like.builder()
                .id(UUID.randomUUID()).userId(uid).postId(pid)
                .reactionType(type).createdAt(LocalDateTime.now()).build();
    }

    private ReactionRequest buildRequest(ReactionType type) {
        ReactionRequest req = new ReactionRequest();
        req.setPostId(postId);
        req.setReactionType(type);
        return req;
    }

    // ── react ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("react: saves new like, increments counter, and notifies post owner")
    void react_newReaction_savesAndNotifies() {
        when(likeRepository.findByUserIdAndPostId(userId, postId)).thenReturn(Optional.empty());
        when(postClient.getPostOwner(postId, "tok")).thenReturn(postOwnerId);
        doNothing().when(postClient).incrementLikes(postId, "tok");

        likeService.react(buildRequest(ReactionType.LIKE), userId, "tok");

        verify(likeRepository).save(any(Like.class));
        verify(postClient).incrementLikes(postId, "tok");
        verify(notificationEventProducer).publishNotificationEvent(any());
    }

    @Test
    @DisplayName("react: does NOT notify when the liker is the post owner")
    void react_noNotifyWhenOwnerLikes() {
        when(likeRepository.findByUserIdAndPostId(userId, postId)).thenReturn(Optional.empty());
        when(postClient.getPostOwner(postId, "tok")).thenReturn(userId);
        doNothing().when(postClient).incrementLikes(postId, "tok");

        likeService.react(buildRequest(ReactionType.LIKE), userId, "tok");

        verify(notificationEventProducer, never()).publishNotificationEvent(any());
    }

    @Test
    @DisplayName("react: only changes reaction type when reaction already exists (no counter, no notification)")
    void react_existingReaction_changesTypeOnly() {
        Like existing = buildLike(userId, postId, ReactionType.LIKE);
        when(likeRepository.findByUserIdAndPostId(userId, postId)).thenReturn(Optional.of(existing));

        likeService.react(buildRequest(ReactionType.LOVE), userId, "tok");

        assertEquals(ReactionType.LOVE, existing.getReactionType());
        verify(likeRepository).save(existing);
        verify(postClient, never()).incrementLikes(any(), any());
        verify(notificationEventProducer, never()).publishNotificationEvent(any());
    }

    @Test
    @DisplayName("react: does not throw when notification publishing fails (fire-and-forget)")
    void react_notificationFails_doesNotThrow() {
        when(likeRepository.findByUserIdAndPostId(userId, postId)).thenReturn(Optional.empty());
        when(postClient.getPostOwner(postId, "tok")).thenReturn(postOwnerId);
        doNothing().when(postClient).incrementLikes(postId, "tok");
        doThrow(new RuntimeException("RabbitMQ down"))
                .when(notificationEventProducer).publishNotificationEvent(any());

        assertDoesNotThrow(() -> likeService.react(buildRequest(ReactionType.LIKE), userId, "tok"));
    }

    // ── removeReaction ────────────────────────────────────────────────────

    @Test
    @DisplayName("removeReaction: deletes like and decrements post counter")
    void removeReaction_success() {
        Like like = buildLike(userId, postId, ReactionType.LIKE);
        when(likeRepository.findByUserIdAndPostId(userId, postId)).thenReturn(Optional.of(like));
        doNothing().when(postClient).decrementLikes(postId, "tok");

        likeService.removeReaction(postId, userId, "tok");

        verify(likeRepository).delete(like);
        verify(postClient).decrementLikes(postId, "tok");
    }

    @Test
    @DisplayName("removeReaction: throws ResourceNotFoundException when reaction not found")
    void removeReaction_notFound_throws() {
        when(likeRepository.findByUserIdAndPostId(userId, postId)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> likeService.removeReaction(postId, userId, "tok"));

        assertEquals("Reaction not found", ex.getMessage());
    }

    // ── hasReacted ────────────────────────────────────────────────────────

    @Test
    @DisplayName("hasReacted: returns true when user has reacted")
    void hasReacted_returnsTrue() {
        when(likeRepository.existsByUserIdAndPostId(userId, postId)).thenReturn(true);
        assertTrue(likeService.hasReacted(postId, userId));
    }

    @Test
    @DisplayName("hasReacted: returns false when user has not reacted")
    void hasReacted_returnsFalse() {
        when(likeRepository.existsByUserIdAndPostId(userId, postId)).thenReturn(false);
        assertFalse(likeService.hasReacted(postId, userId));
    }

    // ── getTotalReactions ─────────────────────────────────────────────────

    @Test
    @DisplayName("getTotalReactions: returns count from repository")
    void getTotalReactions_returnsCount() {
        when(likeRepository.countByPostId(postId)).thenReturn(42L);
        assertEquals(42L, likeService.getTotalReactions(postId));
    }

    // ── getReactionSummary ────────────────────────────────────────────────

    @Test
    @DisplayName("getReactionSummary: groups reactions correctly with correct total")
    void getReactionSummary_groupedCorrectly() {
        List<Like> likes = List.of(
                buildLike(UUID.randomUUID(), postId, ReactionType.LIKE),
                buildLike(UUID.randomUUID(), postId, ReactionType.LIKE),
                buildLike(UUID.randomUUID(), postId, ReactionType.LOVE)
        );
        when(likeRepository.findByPostId(postId)).thenReturn(likes);

        ReactionSummaryResponse summary = likeService.getReactionSummary(postId);

        assertEquals(postId, summary.getPostId());
        assertEquals(3, summary.getTotalCount());
        assertEquals(2L, summary.getReactions().get(ReactionType.LIKE));
        assertEquals(1L, summary.getReactions().get(ReactionType.LOVE));
    }

    @Test
    @DisplayName("getReactionSummary: returns zero total and empty map when no reactions")
    void getReactionSummary_empty() {
        when(likeRepository.findByPostId(postId)).thenReturn(Collections.emptyList());

        ReactionSummaryResponse summary = likeService.getReactionSummary(postId);

        assertEquals(0, summary.getTotalCount());
        assertTrue(summary.getReactions().isEmpty());
    }
}
