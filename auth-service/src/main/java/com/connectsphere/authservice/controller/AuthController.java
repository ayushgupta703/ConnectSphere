package com.connectsphere.authservice.controller;

import com.connectsphere.authservice.dto.*;
import com.connectsphere.authservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${internal.service.secret}")
    private String internalServiceSecret;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(
                authService.getCurrentUser(userDetails.getUsername())
        );
    }

    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(
            @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(
                authService.updateProfile(userDetails.getUsername(), request)
        );
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(authService.getUserById(id));
    }

    @GetMapping("/users/search/username")
    public ResponseEntity<UserResponse> searchUserByUsername(@RequestParam String value) {
        return ResponseEntity.ok(authService.getUserByUsername(value));
    }

    @GetMapping("/users/search/name")
    public ResponseEntity<java.util.List<UserResponse>> searchUsersByName(@RequestParam String value) {
        return ResponseEntity.ok(authService.searchUsersByName(value));
    }

    @GetMapping("/users/search/prefix")
    public ResponseEntity<java.util.List<UserResponse>> searchUsersByUsernamePrefix(@RequestParam String value) {
        return ResponseEntity.ok(authService.searchUsersByUsernamePrefix(value));
    }

    @GetMapping("/users/suggestions")
    public ResponseEntity<java.util.List<UserResponse>> getSuggestions(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) java.util.List<UUID> excludeIds,
            @RequestParam(defaultValue = "5") int limit
    ) {
        UserResponse me = authService.getCurrentUser(userDetails.getUsername());
        return ResponseEntity.ok(authService.getRecommendedUsers(me.id(), excludeIds, limit));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deactivateAccount(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        authService.deactivateAccount(userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    // ========================= INTERNAL (service-to-service) =========================

    /**
     * Returns the subset of provided user IDs that belong to soft-deleted users.
     * Protected by a shared internal secret header — NOT intended for public/frontend use.
     */
    @PostMapping("/users/batch-deleted")
    public ResponseEntity<Set<UUID>> getBatchDeletedUsers(
            @RequestBody Set<UUID> userIds,
            @RequestHeader(value = "X-Internal-Service-Secret", required = false) String secret
    ) {
        if (!internalServiceSecret.equals(secret)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(authService.getBatchDeletedUsers(userIds));
    }
}