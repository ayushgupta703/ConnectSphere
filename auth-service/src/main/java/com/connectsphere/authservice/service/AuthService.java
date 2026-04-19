package com.connectsphere.authservice.service;

import com.connectsphere.authservice.dto.AuthResponse;
import com.connectsphere.authservice.dto.LoginRequest;
import com.connectsphere.authservice.dto.RegisterRequest;
import com.connectsphere.authservice.dto.UserResponse;
import com.connectsphere.authservice.entity.*;
import com.connectsphere.authservice.exception.ConflictException;
import com.connectsphere.authservice.exception.ResourceNotFoundException;
import com.connectsphere.authservice.repository.UserRepository;
import com.connectsphere.authservice.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

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

        String token = jwtService.generateToken(savedUser);

        return new AuthResponse(
                token,
                "Bearer",
                jwtService.getExpirationMs(),
                null, // refreshToken (future)
                toUserResponse(savedUser)
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

        String token = jwtService.generateToken(user);

        return new AuthResponse(
                token,
                "Bearer",
                jwtService.getExpirationMs(),
                null,
                toUserResponse(user)
        );
    }

    // ========================= CURRENT USER =========================
    public UserResponse getCurrentUser(String email) {

        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return toUserResponse(user);
    }

    // ========================= MAPPER =========================
    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getUsername(),
                user.getEmail(),
                user.getBio(),
                user.getProfilePicUrl(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }

    // ========================= UTIL =========================
    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}