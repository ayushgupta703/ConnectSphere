package com.connectsphere.postservice.client;

import com.connectsphere.postservice.dto.response.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.UUID;

@FeignClient(name = "auth-service", url = "http://localhost:8080")
public interface AuthClient {

    @GetMapping("/api/v1/auth/users/{id}")
    UserResponse getUserById(@PathVariable UUID id);

    /**
     * Batch check: returns the subset of provided IDs that belong to soft-deleted users.
     * Requires the X-Internal-Service-Secret header to be accepted by auth-service.
     */
    @PostMapping("/api/v1/auth/users/batch-deleted")
    Set<UUID> getBatchDeletedUsers(
            @RequestBody Set<UUID> userIds,
            @RequestHeader("X-Internal-Service-Secret") String internalSecret
    );
}
