package com.connectsphere.authservice.service;

import com.connectsphere.authservice.entity.RefreshToken;
import com.connectsphere.authservice.entity.User;
import com.connectsphere.authservice.exception.BadRequestException;
import com.connectsphere.authservice.exception.ResourceNotFoundException;
import com.connectsphere.authservice.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshTokenDurationMs;

    // ========================= CREATE =========================
    public RefreshToken createRefreshToken(User user) {

        RefreshToken token = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiryDate(Instant.now().plusMillis(refreshTokenDurationMs))
                .revoked(false)
                .build();

        return refreshTokenRepository.save(token);
    }

    // ========================= VALIDATE =========================
    public RefreshToken verifyToken(String token) {

        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid refresh token"));

        if (refreshToken.getRevoked()) {
            throw new BadRequestException("Refresh token is revoked");
        }

        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new BadRequestException("Refresh token expired");
        }

        return refreshToken;
    }

    // ========================= DELETE =========================
    public void deleteByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }

    // ========================= DELETE BY TOKEN =========================
    public void deleteByToken(String token) {
        refreshTokenRepository.deleteByToken(token);
    }
}