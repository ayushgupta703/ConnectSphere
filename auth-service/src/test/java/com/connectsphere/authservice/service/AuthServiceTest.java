package com.connectsphere.authservice.service;

import com.connectsphere.authservice.dto.*;
import com.connectsphere.authservice.entity.*;
import com.connectsphere.authservice.exception.ConflictException;
import com.connectsphere.authservice.exception.ResourceNotFoundException;
import com.connectsphere.authservice.repository.UserRepository;
import com.connectsphere.authservice.security.JwtService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenService refreshTokenService;

    @InjectMocks private AuthService authService;

    // ── helper ───────────────────────────────────────────────────────────
    private User activeUser(UUID id, String email, String username) {
        User u = new User();
        u.setId(id);
        u.setFullName("Test User");
        u.setEmail(email);
        u.setUsername(username);
        u.setPasswordHash("hashed");
        u.setRole(UserRole.USER);
        u.setStatus(UserStatus.ACTIVE);
        u.setIsActive(true);
        u.setIsDeleted(false);
        u.setProvider(AuthProvider.LOCAL);
        u.setCreatedAt(Instant.now());
        return u;
    }

    private RefreshToken rt(User user, String token) {
        return RefreshToken.builder()
                .id(UUID.randomUUID()).token(token).user(user)
                .expiryDate(Instant.now().plusSeconds(86400)).revoked(false).build();
    }

    // ── register ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("register: returns AuthResponse with correct token and user email")
    void register_success() {
        RegisterRequest req = new RegisterRequest("Test User", "testuser", "test@example.com", "password123");
        UUID id = UUID.randomUUID();
        User saved = activeUser(id, "test@example.com", "testuser");

        when(userRepository.existsByEmailIgnoreCase("test@example.com")).thenReturn(false);
        when(userRepository.existsByUsernameIgnoreCase("testuser")).thenReturn(false);
        when(userRepository.save(any())).thenReturn(saved);
        when(jwtService.generateToken(saved)).thenReturn("access-token");
        when(jwtService.getExpirationMs()).thenReturn(3600000L);
        when(refreshTokenService.createRefreshToken(saved)).thenReturn(rt(saved, "refresh-tok"));

        AuthResponse res = authService.register(req);

        assertNotNull(res);
        assertEquals("access-token", res.token());
        assertEquals("Bearer", res.tokenType());
        assertEquals("refresh-tok", res.refreshToken());
        assertEquals("test@example.com", res.user().email());
    }

    @Test
    @DisplayName("register: throws ConflictException when email already registered")
    void register_emailExists_throws() {
        RegisterRequest req = new RegisterRequest("T", "u", "test@example.com", "password1");
        when(userRepository.existsByEmailIgnoreCase("test@example.com")).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () -> authService.register(req));

        assertEquals("Email is already registered", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register: throws ConflictException when username already taken")
    void register_usernameExists_throws() {
        RegisterRequest req = new RegisterRequest("T", "testuser", "new@example.com", "password1");
        when(userRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
        when(userRepository.existsByUsernameIgnoreCase("testuser")).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () -> authService.register(req));

        assertEquals("Username is already taken", ex.getMessage());
    }

    // ── login ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("login: returns AuthResponse with correct token on valid credentials")
    void login_success() {
        LoginRequest req = new LoginRequest("test@example.com", "password123");
        UUID id = UUID.randomUUID();
        User user = activeUser(id, "test@example.com", "testuser");

        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("access-tok");
        when(jwtService.getExpirationMs()).thenReturn(3600000L);
        when(refreshTokenService.createRefreshToken(user)).thenReturn(rt(user, "refresh-tok"));

        AuthResponse res = authService.login(req);

        assertEquals("access-tok", res.token());
        assertEquals("testuser", res.user().username());
    }

    @Test
    @DisplayName("login: throws BadCredentialsException when user not found")
    void login_userNotFound_throws() {
        LoginRequest req = new LoginRequest("ghost@example.com", "password123");
        when(userRepository.findByEmailIgnoreCase("ghost@example.com")).thenReturn(Optional.empty());

        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> authService.login(req));

        assertEquals("Invalid email or password", ex.getMessage());
    }

    @Test
    @DisplayName("login: throws BadCredentialsException when account is not active")
    void login_accountInactive_throws() {
        LoginRequest req = new LoginRequest("test@example.com", "password123");
        UUID id = UUID.randomUUID();
        User user = activeUser(id, "test@example.com", "testuser");
        user.setStatus(UserStatus.DELETED);
        user.setIsActive(false);

        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(user));

        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> authService.login(req));

        assertEquals("Account is not active", ex.getMessage());
    }

    @Test
    @DisplayName("login: throws BadCredentialsException when password does not match")
    void login_wrongPassword_throws() {
        LoginRequest req = new LoginRequest("test@example.com", "wrongpass");
        UUID id = UUID.randomUUID();
        User user = activeUser(id, "test@example.com", "testuser");

        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpass", "hashed")).thenReturn(false);

        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> authService.login(req));

        assertEquals("Invalid email or password", ex.getMessage());
    }

    // ── logout ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("logout: verifies token and deletes it")
    void logout_success() {
        User user = activeUser(UUID.randomUUID(), "u@e.com", "u");
        String tok = "some-refresh-token";
        RefreshTokenRequest req = new RefreshTokenRequest(tok);
        when(refreshTokenService.verifyToken(tok)).thenReturn(rt(user, tok));

        assertDoesNotThrow(() -> authService.logout(req));

        verify(refreshTokenService).verifyToken(tok);
        verify(refreshTokenService).deleteByToken(tok);
    }

    // ── refreshToken ─────────────────────────────────────────────────────

    @Test
    @DisplayName("refreshToken: returns new access token with same refresh token")
    void refreshToken_success() {
        User user = activeUser(UUID.randomUUID(), "u@e.com", "u");
        String refreshTok = "valid-refresh-tok";
        RefreshToken refreshToken = rt(user, refreshTok);
        RefreshTokenRequest req = new RefreshTokenRequest(refreshTok);

        when(refreshTokenService.verifyToken(refreshTok)).thenReturn(refreshToken);
        when(jwtService.generateToken(user)).thenReturn("new-access-tok");
        when(jwtService.getExpirationMs()).thenReturn(3600000L);

        AuthResponse res = authService.refreshToken(req);

        assertEquals("new-access-tok", res.token());
        assertEquals(refreshTok, res.refreshToken());
    }

    // ── getCurrentUser ───────────────────────────────────────────────────

    @Test
    @DisplayName("getCurrentUser: returns UserResponse for existing user")
    void getCurrentUser_success() {
        UUID id = UUID.randomUUID();
        User user = activeUser(id, "u@e.com", "alice");
        when(userRepository.findByEmailIgnoreCase("u@e.com")).thenReturn(Optional.of(user));

        UserResponse res = authService.getCurrentUser("u@e.com");

        assertEquals("u@e.com", res.email());
        assertEquals(id, res.id());
    }

    @Test
    @DisplayName("getCurrentUser: throws ResourceNotFoundException when user not found")
    void getCurrentUser_notFound_throws() {
        when(userRepository.findByEmailIgnoreCase("ghost@e.com")).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> authService.getCurrentUser("ghost@e.com"));

        assertEquals("User not found", ex.getMessage());
    }

    // ── getUserById ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getUserById: returns UserResponse for non-deleted user")
    void getUserById_success() {
        UUID id = UUID.randomUUID();
        User user = activeUser(id, "u@e.com", "alice");
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        UserResponse res = authService.getUserById(id);

        assertEquals(id, res.id());
    }

    @Test
    @DisplayName("getUserById: throws ResourceNotFoundException for deleted user")
    void getUserById_deletedUser_throws() {
        UUID id = UUID.randomUUID();
        User user = activeUser(id, "u@e.com", "alice");
        user.setIsDeleted(true);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        assertThrows(ResourceNotFoundException.class, () -> authService.getUserById(id));
    }

    @Test
    @DisplayName("getUserById: throws ResourceNotFoundException when user does not exist")
    void getUserById_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.getUserById(id));
    }

    // ── getUserByUsername ─────────────────────────────────────────────────

    @Test
    @DisplayName("getUserByUsername: returns UserResponse for active user")
    void getUserByUsername_success() {
        UUID id = UUID.randomUUID();
        User user = activeUser(id, "u@e.com", "alice");
        when(userRepository.findByUsernameIgnoreCase("alice")).thenReturn(Optional.of(user));

        UserResponse res = authService.getUserByUsername("alice");

        assertEquals("alice", res.username());
    }

    @Test
    @DisplayName("getUserByUsername: throws ResourceNotFoundException for deleted user")
    void getUserByUsername_deletedUser_throws() {
        UUID id = UUID.randomUUID();
        User user = activeUser(id, "u@e.com", "alice");
        user.setIsDeleted(true);
        when(userRepository.findByUsernameIgnoreCase("alice")).thenReturn(Optional.of(user));

        assertThrows(ResourceNotFoundException.class, () -> authService.getUserByUsername("alice"));
    }

    // ── searchUsersByName ─────────────────────────────────────────────────

    @Test
    @DisplayName("searchUsersByName: returns list excluding deleted users")
    void searchUsersByName_returnsFiltered() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        User active = activeUser(id1, "a@e.com", "alice");
        User deleted = activeUser(id2, "d@e.com", "dave");
        deleted.setIsDeleted(true);

        when(userRepository.findByFullNameContainingIgnoreCase("alice"))
                .thenReturn(List.of(active, deleted));

        List<UserResponse> result = authService.searchUsersByName("alice");

        assertEquals(1, result.size());
        assertEquals(id1, result.get(0).id());
    }

    // ── searchUsersByUsernamePrefix ───────────────────────────────────────

    @Test
    @DisplayName("searchUsersByUsernamePrefix: returns list excluding deleted users")
    void searchUsersByUsernamePrefix_returnsFiltered() {
        UUID id1 = UUID.randomUUID();
        User active = activeUser(id1, "a@e.com", "alice");
        when(userRepository.findByUsernameStartingWithIgnoreCase("al")).thenReturn(List.of(active));

        List<UserResponse> result = authService.searchUsersByUsernamePrefix("al");

        assertEquals(1, result.size());
        assertEquals("alice", result.get(0).username());
    }

    // ── getBatchDeletedUsers ──────────────────────────────────────────────

    @Test
    @DisplayName("getBatchDeletedUsers: returns empty set for null input")
    void getBatchDeletedUsers_nullInput_empty() {
        assertTrue(authService.getBatchDeletedUsers(null).isEmpty());
    }

    @Test
    @DisplayName("getBatchDeletedUsers: returns empty set for empty input")
    void getBatchDeletedUsers_emptyInput_empty() {
        assertTrue(authService.getBatchDeletedUsers(Set.of()).isEmpty());
    }

    @Test
    @DisplayName("getBatchDeletedUsers: returns only deleted user IDs")
    void getBatchDeletedUsers_returnsDeletedIds() {
        UUID deletedId = UUID.randomUUID();
        UUID activeId  = UUID.randomUUID();
        User del = activeUser(deletedId, "d@e.com", "del");
        del.setIsDeleted(true);
        User act = activeUser(activeId, "a@e.com", "act");

        when(userRepository.findAllById(Set.of(deletedId, activeId)))
                .thenReturn(List.of(del, act));

        Set<UUID> result = authService.getBatchDeletedUsers(Set.of(deletedId, activeId));

        assertEquals(1, result.size());
        assertTrue(result.contains(deletedId));
    }

    // ── updateProfile ─────────────────────────────────────────────────────

    @Test
    @DisplayName("updateProfile: updates fields and saves")
    void updateProfile_success() {
        UUID id = UUID.randomUUID();
        User user = activeUser(id, "u@e.com", "alice");

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setFullName("Alice Updated");
        req.setBio("Hello!");

        when(userRepository.findByEmail("u@e.com")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        authService.updateProfile("u@e.com", req);

        assertEquals("Alice Updated", user.getFullName());
        assertEquals("Hello!", user.getBio());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("updateProfile: throws RuntimeException when username already taken by another user")
    void updateProfile_usernameConflict_throws() {
        UUID id = UUID.randomUUID();
        User user = activeUser(id, "u@e.com", "alice");

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setUsername("bob");

        when(userRepository.findByEmail("u@e.com")).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("bob")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.updateProfile("u@e.com", req));

        assertEquals("Username already taken", ex.getMessage());
    }

    // ── deactivateAccount ─────────────────────────────────────────────────

    @Test
    @DisplayName("deactivateAccount: marks user deleted and revokes tokens")
    void deactivateAccount_success() {
        UUID id = UUID.randomUUID();
        User user = activeUser(id, "u@e.com", "alice");
        when(userRepository.findByEmail("u@e.com")).thenReturn(Optional.of(user));

        authService.deactivateAccount("u@e.com");

        assertFalse(user.getIsActive());
        assertEquals(UserStatus.DELETED, user.getStatus());
        assertTrue(user.getIsDeleted());
        verify(userRepository).save(user);
        verify(refreshTokenService).deleteByUser(user);
    }

    @Test
    @DisplayName("deactivateAccount: throws ResourceNotFoundException when user not found")
    void deactivateAccount_notFound_throws() {
        when(userRepository.findByEmail("ghost@e.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> authService.deactivateAccount("ghost@e.com"));
    }
}
