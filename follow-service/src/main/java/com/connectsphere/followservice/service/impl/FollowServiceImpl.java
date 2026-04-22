package com.connectsphere.followservice.service.impl;

import com.connectsphere.followservice.dto.*;
import com.connectsphere.followservice.entity.Follow;
import com.connectsphere.followservice.exception.BadRequestException;
import com.connectsphere.followservice.repository.FollowRepository;
import com.connectsphere.followservice.service.FollowService;
import com.connectsphere.followservice.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private final FollowRepository repository;

    @Override
    public FollowResponseDto followUser(UUID targetUserId) {

        UUID currentUserId = SecurityUtils.getCurrentUserId();

        if (currentUserId.equals(targetUserId)) {
            throw new BadRequestException("You cannot follow yourself");
        }

        Follow follow = Follow.builder()
                .followerId(currentUserId)
                .followingId(targetUserId)
                .createdAt(LocalDateTime.now())
                .build();

        Follow saved = repository.save(follow);

        return mapToDto(saved);
    }

    @Override
    public void unfollowUser(UUID targetUserId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        repository.deleteByFollowerIdAndFollowingId(currentUserId, targetUserId);
    }

    @Override
    public Page<FollowResponseDto> getFollowers(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return repository.findByFollowingId(userId, pageable)
                .map(this::mapToDto);
    }

    @Override
    public Page<FollowResponseDto> getFollowing(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return repository.findByFollowerId(userId, pageable)
                .map(this::mapToDto);
    }

    @Override
    public FollowStatusDto isFollowing(UUID targetUserId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        boolean exists = repository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId);

        return new FollowStatusDto(exists);
    }

    private FollowResponseDto mapToDto(Follow follow) {
        return FollowResponseDto.builder()
                .id(follow.getId())
                .followerId(follow.getFollowerId())
                .followingId(follow.getFollowingId())
                .createdAt(follow.getCreatedAt())
                .build();
    }
}