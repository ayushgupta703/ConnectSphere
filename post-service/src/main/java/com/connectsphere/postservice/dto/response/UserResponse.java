package com.connectsphere.postservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private UUID id;
    private String fullName;
    private String username;
    private String email;
    private String bio;
    private String profilePicUrl;
    private String role;
    private String status;
    private Boolean isDeleted;
    private Instant createdAt;
}
