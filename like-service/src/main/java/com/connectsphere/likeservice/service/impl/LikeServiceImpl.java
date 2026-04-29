package com.connectsphere.likeservice.service.impl;

import com.connectsphere.likeservice.client.NotificationClient;
import com.connectsphere.likeservice.client.PostClient;
import com.connectsphere.likeservice.dto.NotificationRequest;
import com.connectsphere.likeservice.dto.ReactionRequest;
import com.connectsphere.likeservice.dto.ReactionSummaryResponse;
import com.connectsphere.likeservice.entity.Like;
import com.connectsphere.likeservice.entity.ReactionType;
import com.connectsphere.likeservice.exception.ResourceNotFoundException;
import com.connectsphere.likeservice.repository.LikeRepository;
import com.connectsphere.likeservice.service.LikeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LikeServiceImpl implements LikeService {

    private final LikeRepository likeRepository;
    private final PostClient postClient;
    private final NotificationClient notificationClient;

    @Override
    public void react(ReactionRequest request, UUID userId, String token) {

        Optional<Like> existing =
                likeRepository.findByUserIdAndPostId(userId, request.getPostId());

        if (existing.isPresent()) {
            // 🔁 CHANGE REACTION (NO NOTIFICATION)
            Like like = existing.get();
            like.setReactionType(request.getReactionType());
            likeRepository.save(like);

        } else {
            // ❤️ NEW REACTION
            Like like = Like.builder()
                    .userId(userId)
                    .postId(request.getPostId())
                    .reactionType(request.getReactionType())
                    .createdAt(LocalDateTime.now())
                    .build();

            likeRepository.save(like);

            // 🔗 CALL POST SERVICE
            postClient.incrementLikes(request.getPostId(), token);

            // 🔥 GET POST OWNER
            UUID postOwnerId = postClient.getPostOwner(request.getPostId(), token);

            // ❌ DON'T NOTIFY SELF
            if (!userId.equals(postOwnerId)) {

                NotificationRequest notification = NotificationRequest.builder()
                        .recipientId(postOwnerId.toString())
                        .actorId(userId.toString())
                        .type("LIKE")
                        .targetId(request.getPostId().toString())
                        .build();

                try {
                    notificationClient.sendNotification(notification, token);
                } catch (Exception e) {
                    log.error("Notification call failed for like on post {} by user {} — continuing",
                            request.getPostId(), userId, e);
                }
            }
        }
    }

    @Override
    public void removeReaction(UUID postId, UUID userId, String token) {

        Like like = likeRepository.findByUserIdAndPostId(userId, postId)
                .orElseThrow(() -> new ResourceNotFoundException("Reaction not found"));

        likeRepository.delete(like);

        // 🔗 CALL POST SERVICE
        postClient.decrementLikes(postId, token);
    }

    @Override
    public boolean hasReacted(UUID postId, UUID userId) {
        return likeRepository.existsByUserIdAndPostId(userId, postId);
    }

    @Override
    public long getTotalReactions(UUID postId) {
        return likeRepository.countByPostId(postId);
    }

    @Override
    public ReactionSummaryResponse getReactionSummary(UUID postId) {

        List<Like> likes = likeRepository.findByPostId(postId);

        Map<ReactionType, Long> map = likes.stream()
                .collect(Collectors.groupingBy(
                        Like::getReactionType,
                        Collectors.counting()
                ));

        return ReactionSummaryResponse.builder()
                .postId(postId)
                .reactions(map)
                .totalCount(likes.size())
                .build();
    }
}