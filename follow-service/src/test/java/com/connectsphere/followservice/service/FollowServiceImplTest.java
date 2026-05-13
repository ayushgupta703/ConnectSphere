package com.connectsphere.followservice.service;

import com.connectsphere.followservice.dto.FollowResponseDto;
import com.connectsphere.followservice.dto.FollowStatusDto;
import com.connectsphere.followservice.entity.Follow;
import com.connectsphere.followservice.event.FollowNotificationEvent;
import com.connectsphere.followservice.exception.BadRequestException;
import com.connectsphere.followservice.producer.NotificationEventProducer;
import com.connectsphere.followservice.repository.FollowRepository;
import com.connectsphere.followservice.service.impl.FollowServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FollowServiceImpl Unit Tests")
class FollowServiceImplTest {

    @Mock private FollowRepository repository;
    @Mock private NotificationEventProducer notificationEventProducer;

    @InjectMocks private FollowServiceImpl followService;

    private UUID currentUserId;
    private UUID targetUserId;

    @BeforeEach
    void setUp() {
        currentUserId = UUID.randomUUID();
        targetUserId  = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(currentUserId.toString(), null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── helper ────────────────────────────────────────────────────────────
    private Follow buildFollow(UUID followerId, UUID followingId) {
        return Follow.builder()
                .id(UUID.randomUUID()).followerId(followerId).followingId(followingId)
                .createdAt(LocalDateTime.now()).build();
    }

    // ── followUser ────────────────────────────────────────────────────────

    @Test
    @DisplayName("followUser: saves follow and publishes FOLLOW notification")
    void followUser_success() {
        when(repository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId)).thenReturn(false);
        when(repository.save(any(Follow.class))).thenReturn(buildFollow(currentUserId, targetUserId));

        FollowResponseDto res = followService.followUser(targetUserId, "tok");

        assertNotNull(res);
        assertEquals(currentUserId, res.getFollowerId());
        assertEquals(targetUserId, res.getFollowingId());
        verify(repository).save(any(Follow.class));
        verify(notificationEventProducer).publishNotificationEvent(
                argThat((FollowNotificationEvent e) -> "FOLLOW".equals(e.getType())));
    }

    @Test
    @DisplayName("followUser: throws BadRequestException when trying to follow yourself")
    void followUser_selfFollow_throws() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(targetUserId.toString(), null, List.of()));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> followService.followUser(targetUserId, "tok"));

        assertTrue(ex.getMessage().contains("cannot follow yourself"));
    }

    @Test
    @DisplayName("followUser: throws BadRequestException when already following")
    void followUser_alreadyFollowing_throws() {
        when(repository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId)).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> followService.followUser(targetUserId, "tok"));

        assertTrue(ex.getMessage().contains("Already following"));
    }

    @Test
    @DisplayName("followUser: does not throw when notification publishing fails")
    void followUser_notificationFails_doesNotThrow() {
        when(repository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId)).thenReturn(false);
        when(repository.save(any())).thenReturn(buildFollow(currentUserId, targetUserId));
        doThrow(new RuntimeException("RabbitMQ error"))
                .when(notificationEventProducer).publishNotificationEvent(any());

        assertDoesNotThrow(() -> followService.followUser(targetUserId, "tok"));
    }

    // ── unfollowUser ──────────────────────────────────────────────────────

    @Test
    @DisplayName("unfollowUser: delegates delete to repository")
    void unfollowUser_success() {
        followService.unfollowUser(targetUserId, currentUserId);
        verify(repository).deleteByFollowerIdAndFollowingId(currentUserId, targetUserId);
    }

    // ── getFollowers ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getFollowers: returns paginated followers with correct followerId")
    void getFollowers_success() {
        Pageable pageable = PageRequest.of(0, 10);
        when(repository.findByFollowingId(targetUserId, pageable))
                .thenReturn(new PageImpl<>(List.of(buildFollow(currentUserId, targetUserId))));

        Page<FollowResponseDto> result = followService.getFollowers(targetUserId, 0, 10);

        assertEquals(1, result.getContent().size());
        assertEquals(currentUserId, result.getContent().get(0).getFollowerId());
    }

    // ── getFollowing ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getFollowing: returns paginated following list with correct followingId")
    void getFollowing_success() {
        Pageable pageable = PageRequest.of(0, 10);
        when(repository.findByFollowerId(currentUserId, pageable))
                .thenReturn(new PageImpl<>(List.of(buildFollow(currentUserId, targetUserId))));

        Page<FollowResponseDto> result = followService.getFollowing(currentUserId, 0, 10);

        assertEquals(1, result.getContent().size());
        assertEquals(targetUserId, result.getContent().get(0).getFollowingId());
    }

    // ── getFollowingIds ───────────────────────────────────────────────────

    @Test
    @DisplayName("getFollowingIds: returns list containing the followed user's UUID")
    void getFollowingIds_success() {
        when(repository.findByFollowerId(currentUserId))
                .thenReturn(List.of(buildFollow(currentUserId, targetUserId)));

        List<UUID> ids = followService.getFollowingIds(currentUserId);

        assertEquals(1, ids.size());
        assertEquals(targetUserId, ids.get(0));
    }

    // ── isFollowing ───────────────────────────────────────────────────────

    @Test
    @DisplayName("isFollowing: returns true when follow relationship exists")
    void isFollowing_returnsTrue() {
        when(repository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId)).thenReturn(true);

        FollowStatusDto status = followService.isFollowing(targetUserId);

        assertTrue(status.isFollowing());
    }

    @Test
    @DisplayName("isFollowing: returns false when follow relationship does not exist")
    void isFollowing_returnsFalse() {
        when(repository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId)).thenReturn(false);

        FollowStatusDto status = followService.isFollowing(targetUserId);

        assertFalse(status.isFollowing());
    }
}
