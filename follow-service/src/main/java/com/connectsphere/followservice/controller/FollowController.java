package com.connectsphere.followservice.controller;

import com.connectsphere.followservice.dto.*;
import com.connectsphere.followservice.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/follows")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @PostMapping("/{userId}")
    public FollowResponseDto followUser(@PathVariable UUID userId) {
        return followService.followUser(userId);
    }

    @DeleteMapping("/{userId}")
    public void unfollowUser(@PathVariable UUID userId) {
        followService.unfollowUser(userId);
    }

    @GetMapping("/{userId}/followers")
    public Page<FollowResponseDto> getFollowers(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return followService.getFollowers(userId, page, size);
    }

    @GetMapping("/{userId}/following")
    public Page<FollowResponseDto> getFollowing(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return followService.getFollowing(userId, page, size);
    }

    @GetMapping("/{userId}/is-following")
    public FollowStatusDto isFollowing(@PathVariable UUID userId) {
        return followService.isFollowing(userId);
    }
}