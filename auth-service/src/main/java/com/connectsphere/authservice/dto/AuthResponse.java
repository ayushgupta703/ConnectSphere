package com.connectsphere.authservice.dto;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresInMs,
        String refreshToken,
        UserResponse user
) {
}