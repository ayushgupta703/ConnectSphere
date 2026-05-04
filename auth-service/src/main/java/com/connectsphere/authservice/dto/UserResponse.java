package com.connectsphere.authservice.dto;

import com.connectsphere.authservice.entity.UserRole;
import com.connectsphere.authservice.entity.UserStatus;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String fullName,
        String username,
        String email,
        String bio,
        String profilePicUrl,
        UserRole role,
        UserStatus status,
        Boolean isDeleted,
        Instant createdAt
) {
}