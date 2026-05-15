package com.connectsphere.followservice.service;

import com.connectsphere.followservice.dto.FollowResponseDto;
import com.connectsphere.followservice.dto.FollowStatusDto;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface FollowService {

    FollowResponseDto followUser(UUID targetUserId, String token);

    void unfollowUser(UUID targetUserId, UUID currentUserId);

    Page<FollowResponseDto> getFollowers(UUID userId, int page, int size);

    Page<FollowResponseDto> getFollowing(UUID userId, int page, int size);

    List<UUID> getFollowingIds(UUID userId);

    FollowStatusDto isFollowing(UUID targetUserId);
}