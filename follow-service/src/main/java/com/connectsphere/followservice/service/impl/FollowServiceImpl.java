package com.connectsphere.followservice.service.impl;

import com.connectsphere.followservice.client.NotificationClient;
import com.connectsphere.followservice.dto.*;
import com.connectsphere.followservice.entity.Follow;
import com.connectsphere.followservice.exception.BadRequestException;
import com.connectsphere.followservice.repository.FollowRepository;
import com.connectsphere.followservice.service.FollowService;
import com.connectsphere.followservice.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private final FollowRepository repository;
    private final NotificationClient notificationClient;

    @Override
    public FollowResponseDto followUser(UUID targetUserId, String token) {

        UUID currentUserId = SecurityUtils.getCurrentUserId();

        if (currentUserId.equals(targetUserId)) {
            throw new BadRequestException("You cannot follow yourself");
        }

        // ❗ OPTIONAL: prevent duplicate follow (recommended)
        boolean alreadyFollowing = repository
                .existsByFollowerIdAndFollowingId(currentUserId, targetUserId);

        if (alreadyFollowing) {
            throw new BadRequestException("Already following this user");
        }

        Follow follow = Follow.builder()
                .followerId(currentUserId)
                .followingId(targetUserId)
                .createdAt(LocalDateTime.now())
                .build();

        Follow saved = repository.save(follow);

        // 🔥 SEND NOTIFICATION (non-blocking — notification-service failures must not break the follow action)
        try {
            notificationClient.sendNotification(
                    com.connectsphere.followservice.dto.NotificationRequest.builder()
                            .recipientId(targetUserId.toString())
                            .actorId(currentUserId.toString())
                            .type("FOLLOW")
                            .targetId(targetUserId.toString())
                            .build(),
                    token
            );
        } catch (Exception e) {
            log.error("Notification delivery failed for FOLLOW event: followerId={}, targetId={} — {}",
                    currentUserId, targetUserId, e.getMessage());
        }

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public void unfollowUser(UUID targetUserId, UUID currentUserId) {
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
    public List<UUID> getFollowingIds(UUID userId) {
        return repository.findByFollowerId(userId)
                .stream()
                .map(follow -> follow.getFollowingId())
                .collect(Collectors.toList());
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