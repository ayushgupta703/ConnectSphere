package com.connectsphere.notificationservice.service;

import com.connectsphere.notificationservice.dto.NotificationRequest;
import com.connectsphere.notificationservice.entity.Notification;
import com.connectsphere.notificationservice.entity.NotificationType;
import com.connectsphere.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationServiceImpl Unit Tests")
class NotificationServiceImplTest {

    @Mock private NotificationRepository repository;
    @InjectMocks private NotificationServiceImpl notificationService;

    // ── helper ────────────────────────────────────────────────────────────
    private Notification buildNotification(String id, String recipientId, boolean read) {
        Notification n = Notification.builder()
                .recipientId(recipientId).actorId("actor-uuid")
                .type(NotificationType.LIKE).targetId("post-uuid")
                .targetType("POST").message("Someone liked your post").build();
        try {
            var fId = Notification.class.getDeclaredField("id");
            fId.setAccessible(true);
            fId.set(n, id);
            var fRead = Notification.class.getDeclaredField("isRead");
            fRead.setAccessible(true);
            fRead.set(n, read);
        } catch (Exception e) { throw new RuntimeException(e); }
        return n;
    }

    private NotificationRequest buildRequest(String type) {
        return NotificationRequest.builder()
                .recipientId("recipient-id").actorId("actor-id")
                .type(type).targetId("target-id").build();
    }

    // ── getUserNotifications ──────────────────────────────────────────────

    @Test
    @DisplayName("getUserNotifications: returns all notifications for the user")
    void getUserNotifications_returnsList() {
        when(repository.findByRecipientId("user-1"))
                .thenReturn(List.of(
                        buildNotification("id-1", "user-1", false),
                        buildNotification("id-2", "user-1", true)));

        List<Notification> result = notificationService.getUserNotifications("user-1");

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("getUserNotifications: returns empty list when user has no notifications")
    void getUserNotifications_emptyList() {
        when(repository.findByRecipientId("nobody")).thenReturn(List.of());
        assertEquals(0, notificationService.getUserNotifications("nobody").size());
    }

    // ── markAsRead ────────────────────────────────────────────────────────

    @Test
    @DisplayName("markAsRead: sets isRead to true and saves")
    void markAsRead_success() {
        Notification n = buildNotification("notif-1", "user-1", false);
        when(repository.findById("notif-1")).thenReturn(Optional.of(n));

        notificationService.markAsRead("notif-1");

        assertTrue(n.isRead());
        verify(repository).save(n);
    }

    @Test
    @DisplayName("markAsRead: throws RuntimeException when notification not found")
    void markAsRead_notFound_throws() {
        when(repository.findById("x")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> notificationService.markAsRead("x"));

        assertEquals("Notification not found", ex.getMessage());
    }

    // ── markAllAsRead ─────────────────────────────────────────────────────

    @Test
    @DisplayName("markAllAsRead: marks all unread notifications as read and saves all")
    void markAllAsRead_success() {
        Notification n1 = buildNotification("id-1", "user-1", false);
        Notification n2 = buildNotification("id-2", "user-1", false);
        when(repository.findByRecipientIdAndIsRead("user-1", false)).thenReturn(List.of(n1, n2));

        notificationService.markAllAsRead("user-1");

        assertTrue(n1.isRead());
        assertTrue(n2.isRead());
        verify(repository).saveAll(List.of(n1, n2));
    }

    @Test
    @DisplayName("markAllAsRead: does nothing when there are no unread notifications")
    void markAllAsRead_nothingToMark() {
        when(repository.findByRecipientIdAndIsRead("user-1", false)).thenReturn(List.of());
        notificationService.markAllAsRead("user-1");
        verify(repository).saveAll(List.of());
    }

    // ── getUnreadCount ────────────────────────────────────────────────────

    @Test
    @DisplayName("getUnreadCount: returns count from repository")
    void getUnreadCount_returnsCount() {
        when(repository.countByRecipientIdAndIsRead("user-1", false)).thenReturn(5L);
        assertEquals(5L, notificationService.getUnreadCount("user-1"));
    }

    // ── createNotification ────────────────────────────────────────────────

    @Test
    @DisplayName("createNotification: LIKE saves correct type, message, and all IDs")
    void createNotification_like() {
        ArgumentCaptor<Notification> cap = ArgumentCaptor.forClass(Notification.class);
        notificationService.createNotification(buildRequest("LIKE"));
        verify(repository).save(cap.capture());

        Notification saved = cap.getValue();
        assertEquals(NotificationType.LIKE, saved.getType());
        assertEquals("Someone liked your post", saved.getMessage());
        assertEquals("recipient-id", saved.getRecipientId());
        assertEquals("actor-id", saved.getActorId());
        assertEquals("target-id", saved.getTargetId());
        assertEquals("POST", saved.getTargetType());
    }

    @Test
    @DisplayName("createNotification: COMMENT saves correct type and message")
    void createNotification_comment() {
        ArgumentCaptor<Notification> cap = ArgumentCaptor.forClass(Notification.class);
        notificationService.createNotification(buildRequest("COMMENT"));
        verify(repository).save(cap.capture());

        assertEquals(NotificationType.COMMENT, cap.getValue().getType());
        assertEquals("Someone commented on your post", cap.getValue().getMessage());
    }

    @Test
    @DisplayName("createNotification: REPLY saves correct type and message")
    void createNotification_reply() {
        ArgumentCaptor<Notification> cap = ArgumentCaptor.forClass(Notification.class);
        notificationService.createNotification(buildRequest("REPLY"));
        verify(repository).save(cap.capture());

        assertEquals(NotificationType.REPLY, cap.getValue().getType());
        assertEquals("Someone replied to your comment", cap.getValue().getMessage());
    }

    @Test
    @DisplayName("createNotification: FOLLOW saves correct type and message")
    void createNotification_follow() {
        ArgumentCaptor<Notification> cap = ArgumentCaptor.forClass(Notification.class);
        notificationService.createNotification(buildRequest("FOLLOW"));
        verify(repository).save(cap.capture());

        assertEquals(NotificationType.FOLLOW, cap.getValue().getType());
        assertEquals("Someone started following you", cap.getValue().getMessage());
    }

    @Test
    @DisplayName("createNotification: unknown type saves default message")
    void createNotification_unknown_savesDefault() {
        ArgumentCaptor<Notification> cap = ArgumentCaptor.forClass(Notification.class);
        notificationService.createNotification(buildRequest("MENTION"));
        verify(repository).save(cap.capture());

        assertEquals("New notification", cap.getValue().getMessage());
    }

    @Test
    @DisplayName("createNotification: strips surrounding quotes from IDs (clean() guard)")
    void createNotification_stripsQuotesFromIds() {
        NotificationRequest req = NotificationRequest.builder()
                .recipientId("\"uuid-with-quotes\"")
                .actorId("\"actor-uuid\"")
                .type("LIKE")
                .targetId("\"post-uuid\"")
                .build();

        ArgumentCaptor<Notification> cap = ArgumentCaptor.forClass(Notification.class);
        notificationService.createNotification(req);
        verify(repository).save(cap.capture());

        assertEquals("uuid-with-quotes", cap.getValue().getRecipientId());
        assertEquals("actor-uuid", cap.getValue().getActorId());
        assertEquals("post-uuid", cap.getValue().getTargetId());
    }

    @Test
    @DisplayName("createNotification: handles null ID fields without throwing")
    void createNotification_nullIds_handledGracefully() {
        NotificationRequest req = NotificationRequest.builder()
                .recipientId(null).actorId(null).type("LIKE").targetId(null).build();

        ArgumentCaptor<Notification> cap = ArgumentCaptor.forClass(Notification.class);
        notificationService.createNotification(req);
        verify(repository).save(cap.capture());

        assertNull(cap.getValue().getRecipientId());
        assertNull(cap.getValue().getActorId());
    }
}
