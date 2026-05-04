package com.connectsphere.authservice.service;

import com.connectsphere.authservice.dto.*;
import com.connectsphere.authservice.entity.*;
import com.connectsphere.authservice.exception.ConflictException;
import com.connectsphere.authservice.exception.ResourceNotFoundException;
import com.connectsphere.authservice.repository.UserRepository;
import com.connectsphere.authservice.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    // ========================= REGISTER =========================
    @Transactional
    public AuthResponse register(RegisterRequest request) {

        String email = normalizeEmail(request.email());
        String username = request.username().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email is already registered");
        }

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ConflictException("Username is already taken");
        }

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));

        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setIsActive(true);
        user.setProvider(AuthProvider.LOCAL);

        User savedUser = userRepository.save(user);

        String accessToken = jwtService.generateToken(savedUser);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(savedUser);

        return new AuthResponse(
                accessToken,
                "Bearer",
                jwtService.getExpirationMs(),
                refreshToken.getToken(),
                mapToUserResponse(savedUser)
        );
    }

    // ========================= LOGIN =========================
    public AuthResponse login(LoginRequest request) {

        String email = normalizeEmail(request.email());

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        // IMPORTANT: status check
        if (user.getStatus() != UserStatus.ACTIVE || Boolean.FALSE.equals(user.getIsActive())) {
            throw new BadCredentialsException("Account is not active");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String accessToken = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return new AuthResponse(
                accessToken,
                "Bearer",
                jwtService.getExpirationMs(),
                refreshToken.getToken(),
                mapToUserResponse(user)
        );
    }

    // ========================= LOGOUT =========================
    @Transactional
    public void logout(RefreshTokenRequest request) {

        String requestToken = request.refreshToken();

        // Validate token first
        refreshTokenService.verifyToken(requestToken);

        // Delete token
        refreshTokenService.deleteByToken(requestToken);
    }

    // ========================= REFRESH TOKEN =========================
    @Transactional
    public AuthResponse refreshToken(@Valid RefreshTokenRequest request) {
        String requestToken = request.refreshToken();
        RefreshToken refreshToken = refreshTokenService.verifyToken(requestToken);
        User user = refreshToken.getUser();
        String accessToken = jwtService.generateToken(user);
        return new AuthResponse(
                accessToken,
                "Bearer",
                jwtService.getExpirationMs(),
                requestToken,
                mapToUserResponse(user)
        );
    }

    // ========================= CURRENT USER =========================
    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return mapToUserResponse(user);
    }

    public UserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .filter(u -> !Boolean.TRUE.equals(u.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return mapToUserResponse(user);
    }


    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsernameIgnoreCase(username)
                .filter(u -> !Boolean.TRUE.equals(u.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return mapToUserResponse(user);
    }

    public java.util.List<UserResponse> searchUsersByName(String name) {
        return userRepository.findByFullNameContainingIgnoreCase(name)
                .stream()
                .filter(user -> !Boolean.TRUE.equals(user.getIsDeleted()))
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    // ========================= SEARCH USERS =========================
    public java.util.List<UserResponse> searchUsersByUsernamePrefix(String prefix) {
        return userRepository.findByUsernameStartingWithIgnoreCase(prefix)
                .stream()
                .filter(user -> !Boolean.TRUE.equals(user.getIsDeleted()))
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    // ========================= BATCH DELETED USERS =========================
    public Set<UUID> getBatchDeletedUsers(Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        return userRepository.findAllById(ids)
                .stream()
                .filter(user -> Boolean.TRUE.equals(user.getIsDeleted()))
                .map(User::getId)
                .collect(Collectors.toSet());
    }

    @Transactional
    public UserResponse updateProfile(String username, UpdateProfileRequest request) {
        log.info("Updating profile for user: {}", username);
        log.debug("Update request payload: {}", request);

        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Unique check for username
        if (request.getUsername() != null &&
                userRepository.existsByUsername(request.getUsername()) &&
                !user.getUsername().equals(request.getUsername())) {
            throw new RuntimeException("Username already taken");
        }

        // Unique check for email
        if (request.getEmail() != null &&
                userRepository.existsByEmail(request.getEmail()) &&
                !user.getEmail().equals(request.getEmail())) {
            throw new RuntimeException("Email already in use");
        }

        // 🔥 FULL UPDATE LOGIC
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }

        if (request.getUsername() != null) {
            user.setUsername(request.getUsername());
        }

        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }

        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }

        if (request.getProfilePicUrl() != null) {
            user.setProfilePicUrl(request.getProfilePicUrl());
        }

        userRepository.save(user);

        return mapToUserResponse(user);
    }

    @Transactional
    public void deactivateAccount(String username) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setIsActive(false);
        user.setStatus(UserStatus.DELETED);
        user.setIsDeleted(true);
        userRepository.save(user);

        // Optionally, invalidate all refresh tokens for this user
        refreshTokenService.deleteByUser(user);
    }

    // ========================= MAPPER =========================

    private UserResponse mapToUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getUsername(),
                user.getEmail(),
                user.getBio(),
                user.getProfilePicUrl(),
                user.getRole(),
                user.getStatus(),
                user.getIsDeleted(),
                user.getCreatedAt()
        );
    }

    // ========================= UTIL =========================
    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

}