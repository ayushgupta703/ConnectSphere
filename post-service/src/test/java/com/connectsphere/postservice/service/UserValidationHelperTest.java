package com.connectsphere.postservice.service;

import com.connectsphere.postservice.client.AuthClient;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserValidationHelper Unit Tests")
class UserValidationHelperTest {

    @Mock private AuthClient authClient;
    @InjectMocks private UserValidationHelper userValidationHelper;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userValidationHelper, "internalSecret", "test-secret");
    }

    // ── getDeletedUserIds ─────────────────────────────────────────────────

    @Test
    @DisplayName("getDeletedUserIds: returns empty set for null input without calling authClient")
    void getDeletedUserIds_nullInput_empty() {
        Set<UUID> result = userValidationHelper.getDeletedUserIds(null);
        assertTrue(result.isEmpty());
        verifyNoInteractions(authClient);
    }

    @Test
    @DisplayName("getDeletedUserIds: returns empty set for empty input without calling authClient")
    void getDeletedUserIds_emptyInput_empty() {
        Set<UUID> result = userValidationHelper.getDeletedUserIds(Set.of());
        assertTrue(result.isEmpty());
        verifyNoInteractions(authClient);
    }

    @Test
    @DisplayName("getDeletedUserIds: calls authClient and returns only deleted IDs")
    void getDeletedUserIds_returnsDeletedIds() {
        UUID deletedId = UUID.randomUUID();
        UUID activeId  = UUID.randomUUID();
        Set<UUID> input = Set.of(deletedId, activeId);

        when(authClient.getBatchDeletedUsers(input, "test-secret")).thenReturn(Set.of(deletedId));

        Set<UUID> result = userValidationHelper.getDeletedUserIds(input);

        assertEquals(1, result.size());
        assertTrue(result.contains(deletedId));
        verify(authClient).getBatchDeletedUsers(input, "test-secret");
    }

    @Test
    @DisplayName("getDeletedUserIds: caches result and calls authClient only once for the same input")
    void getDeletedUserIds_cachesResult() {
        UUID id = UUID.randomUUID();
        when(authClient.getBatchDeletedUsers(Set.of(id), "test-secret")).thenReturn(Set.of());

        userValidationHelper.getDeletedUserIds(Set.of(id));
        userValidationHelper.getDeletedUserIds(Set.of(id));

        verify(authClient, times(1)).getBatchDeletedUsers(any(), any());
    }

    @Test
    @DisplayName("getDeletedUserIds: returns empty set when authClient throws (graceful degradation)")
    void getDeletedUserIds_authClientFails_returnsEmpty() {
        UUID id = UUID.randomUUID();
        when(authClient.getBatchDeletedUsers(any(), any()))
                .thenThrow(new RuntimeException("auth-service down"));

        Set<UUID> result = userValidationHelper.getDeletedUserIds(Set.of(id));

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getDeletedUserIds: returns empty set when authClient returns null")
    void getDeletedUserIds_nullResponse_returnsEmpty() {
        UUID id = UUID.randomUUID();
        when(authClient.getBatchDeletedUsers(any(), any())).thenReturn(null);

        Set<UUID> result = userValidationHelper.getDeletedUserIds(Set.of(id));

        assertTrue(result.isEmpty());
    }

    // ── isOwnerDeleted ────────────────────────────────────────────────────

    @Test
    @DisplayName("isOwnerDeleted: returns true when owner is in deleted set")
    void isOwnerDeleted_returns_true() {
        UUID userId = UUID.randomUUID();
        when(authClient.getBatchDeletedUsers(Set.of(userId), "test-secret")).thenReturn(Set.of(userId));

        assertTrue(userValidationHelper.isOwnerDeleted(userId));
    }

    @Test
    @DisplayName("isOwnerDeleted: returns false when owner is not in deleted set")
    void isOwnerDeleted_returns_false() {
        UUID userId = UUID.randomUUID();
        when(authClient.getBatchDeletedUsers(Set.of(userId), "test-secret")).thenReturn(Set.of());

        assertFalse(userValidationHelper.isOwnerDeleted(userId));
    }
}
