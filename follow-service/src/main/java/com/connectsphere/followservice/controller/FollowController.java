package com.connectsphere.followservice.controller;

import com.connectsphere.followservice.dto.*;
import com.connectsphere.followservice.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/follows")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @PostMapping("/{userId}")
    public ResponseEntity<FollowResponseDto> followUser(@PathVariable UUID userId,
                                                        @RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(followService.followUser(userId, token));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> unfollowUser(@PathVariable UUID userId,
                                             @AuthenticationPrincipal String currentUserId) {
        followService.unfollowUser(userId, UUID.fromString(currentUserId));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}/followers")
    public ResponseEntity<Page<FollowResponseDto>> getFollowers(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(followService.getFollowers(userId, page, size));
    }

    @GetMapping("/{userId}/following")
    public ResponseEntity<Page<FollowResponseDto>> getFollowing(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(followService.getFollowing(userId, page, size));
    }

    @GetMapping("/{userId}/is-following")
    public ResponseEntity<FollowStatusDto> isFollowing(@PathVariable UUID userId) {
        return ResponseEntity.ok(followService.isFollowing(userId));
    }

    // 🔥 Internal endpoint for Feign inter-service calls (returns flat UUID list)
    @GetMapping("/following/{userId}")
    public ResponseEntity<List<UUID>> getFollowingIds(@PathVariable UUID userId) {
        return ResponseEntity.ok(followService.getFollowingIds(userId));
    }
}