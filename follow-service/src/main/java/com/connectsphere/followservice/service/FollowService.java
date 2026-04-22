package com.connectsphere.followservice.service;

import com.connectsphere.followservice.dto.FollowResponseDto;
import com.connectsphere.followservice.dto.FollowStatusDto;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface FollowService {

    FollowResponseDto followUser(UUID targetUserId);

    void unfollowUser(UUID targetUserId);

    Page<FollowResponseDto> getFollowers(UUID userId, int page, int size);

    Page<FollowResponseDto> getFollowing(UUID userId, int page, int size);

    FollowStatusDto isFollowing(UUID targetUserId);
}