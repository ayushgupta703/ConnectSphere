package com.connectsphere.authservice.service;

import com.connectsphere.authservice.entity.RefreshToken;
import com.connectsphere.authservice.entity.User;
import com.connectsphere.authservice.exception.BadRequestException;
import com.connectsphere.authservice.exception.ResourceNotFoundException;
import com.connectsphere.authservice.repository.RefreshTokenRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService Unit Tests")
class RefreshTokenServiceTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @InjectMocks private RefreshTokenService refreshTokenService;

    private static final long REFRESH_EXPIRY_MS = 86_400_000L;

    private User buildUser() {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail("u@e.com");
        u.setUsername("u");
        return u;
    }

    private RefreshToken buildToken(User user, boolean revoked, Instant expiry) {
        return RefreshToken.builder()
                .id(UUID.randomUUID())
                .token(UUID.randomUUID().toString())
                .user(user)
                .revoked(revoked)
                .expiryDate(expiry)
                .build();
    }

    // ── createRefreshToken ────────────────────────────────────────────────

    @Test
    @DisplayName("createRefreshToken: saves and returns a non-revoked token linked to the user")
    void createRefreshToken_success() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenDurationMs", REFRESH_EXPIRY_MS);
        User user = buildUser();
        RefreshToken persisted = buildToken(user, false, Instant.now().plusMillis(REFRESH_EXPIRY_MS));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(persisted);

        RefreshToken result = refreshTokenService.createRefreshToken(user);

        assertNotNull(result);
        assertFalse(result.getRevoked());
        assertEquals(user, result.getUser());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    // ── verifyToken ───────────────────────────────────────────────────────

    @Test
    @DisplayName("verifyToken: returns token when valid, not revoked, and not expired")
    void verifyToken_success() {
        User user = buildUser();
        RefreshToken token = buildToken(user, false, Instant.now().plusSeconds(3600));
        when(refreshTokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));

        RefreshToken result = refreshTokenService.verifyToken(token.getToken());

        assertEquals(token, result);
    }

    @Test
    @DisplayName("verifyToken: throws ResourceNotFoundException when token not found")
    void verifyToken_notFound_throws() {
        when(refreshTokenRepository.findByToken("invalid")).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> refreshTokenService.verifyToken("invalid"));

        assertEquals("Invalid refresh token", ex.getMessage());
    }

    @Test
    @DisplayName("verifyToken: throws BadRequestException when token is revoked")
    void verifyToken_revoked_throws() {
        User user = buildUser();
        RefreshToken token = buildToken(user, true, Instant.now().plusSeconds(3600));
        when(refreshTokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> refreshTokenService.verifyToken(token.getToken()));

        assertTrue(ex.getMessage().contains("revoked"));
    }

    @Test
    @DisplayName("verifyToken: deletes token and throws BadRequestException when expired")
    void verifyToken_expired_deletesAndThrows() {
        User user = buildUser();
        RefreshToken token = buildToken(user, false, Instant.now().minusSeconds(10));
        when(refreshTokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> refreshTokenService.verifyToken(token.getToken()));

        assertTrue(ex.getMessage().contains("expired"));
        verify(refreshTokenRepository).delete(token);
    }

    // ── deleteByUser / deleteByToken ──────────────────────────────────────

    @Test
    @DisplayName("deleteByUser: delegates to repository")
    void deleteByUser_callsRepository() {
        User user = buildUser();
        refreshTokenService.deleteByUser(user);
        verify(refreshTokenRepository).deleteByUser(user);
    }

    @Test
    @DisplayName("deleteByToken: delegates to repository")
    void deleteByToken_callsRepository() {
        refreshTokenService.deleteByToken("some-token");
        verify(refreshTokenRepository).deleteByToken("some-token");
    }
}
