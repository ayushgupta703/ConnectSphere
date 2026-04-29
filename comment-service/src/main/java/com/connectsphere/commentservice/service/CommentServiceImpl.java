package com.connectsphere.commentservice.service;

import com.connectsphere.commentservice.client.NotificationClient;
import com.connectsphere.commentservice.client.PostClient;
import com.connectsphere.commentservice.dto.CommentResponseDto;
import com.connectsphere.commentservice.entity.Comment;
import com.connectsphere.commentservice.exception.BadRequestException;
import com.connectsphere.commentservice.exception.ResourceNotFoundException;
import com.connectsphere.commentservice.exception.UnauthorizedException;
import com.connectsphere.commentservice.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final PostClient postClient;
    private final NotificationClient notificationClient;

    // ✅ ADD COMMENT
    @Override
    @Transactional
    public CommentResponseDto addComment(UUID userId, UUID postId, String content, UUID parentCommentId, String token) {

        Comment parent = null;

        if (parentCommentId != null) {
            parent = commentRepository.findByIdAndDeletedFalse(parentCommentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Parent comment not found"));

            if (parent.getParentCommentId() != null) {
                throw new BadRequestException("Only 2-level comments allowed");
            }

            if (!parent.getPostId().equals(postId)) {
                throw new ResourceNotFoundException("Invalid parent comment");
            }
        }

        Comment comment = Comment.builder()
                .userId(userId)
                .postId(postId)
                .parentCommentId(parentCommentId)
                .content(content)
                .likesCount(0)
                .deleted(false)
                .build();

        Comment saved = commentRepository.save(comment);

        // 🔗 Feign call to Post Service
        postClient.incrementComments(postId, token);

        // 🔥 NOTIFICATION LOGIC STARTS HERE

        if (parentCommentId == null) {
            // 📌 CASE 1: COMMENT ON POST

            UUID postOwnerId = postClient.getPostOwner(postId, token);

            if (!postOwnerId.equals(userId)) {

                notificationClient.sendNotification(
                        com.connectsphere.commentservice.dto.NotificationRequest.builder()
                                .recipientId(postOwnerId.toString())
                                .actorId(userId.toString())
                                .type("COMMENT")
                                .targetId(postId.toString())
                                .build(),
                        token
                );
            }

        } else {
            // 📌 CASE 2: REPLY TO COMMENT

            UUID commentOwnerId = parent.getUserId();

            if (!commentOwnerId.equals(userId)) {

                notificationClient.sendNotification(
                        com.connectsphere.commentservice.dto.NotificationRequest.builder()
                                .recipientId(commentOwnerId.toString())
                                .actorId(userId.toString())
                                .type("REPLY")
                                .targetId(parentCommentId.toString())
                                .build(),
                        token
                );
            }
        }

        // 🔥 NOTIFICATION LOGIC ENDS HERE

        return mapToResponse(saved);
    }

    // ✅ DELETE COMMENT (SOFT DELETE)
    @Override
    @Transactional
    public void deleteComment(UUID commentId, UUID userId, String token) {

        Comment comment = commentRepository.findByIdAndDeletedFalse(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        if (!comment.getUserId().equals(userId)) {
            throw new UnauthorizedException("Not allowed");
        }

        comment.setDeleted(true);
        commentRepository.save(comment);

        // 🔥 Feign call
        postClient.decrementComments(comment.getPostId(), token);
    }

    // ✅ GET COMMENTS (TOP-LEVEL)
    @Override
    @Transactional(readOnly = true)
    public Page<CommentResponseDto> getComments(UUID postId, int page, int size) {

        return commentRepository
                .findByPostIdAndParentCommentIdIsNullAndDeletedFalse(postId, PageRequest.of(page, size))
                .map(this::mapToResponse);
    }

    // ✅ GET REPLIES
    @Override
    @Transactional(readOnly = true)
    public List<CommentResponseDto> getReplies(UUID commentId) {

        return commentRepository
                .findByParentCommentIdAndDeletedFalse(commentId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // ✅ COUNT COMMENTS
    @Override
    @Transactional(readOnly = true)
    public long countComments(UUID postId) {
        return commentRepository.countByPostIdAndDeletedFalse(postId);
    }

    // 🔥 UPDATE COMMENT
    @Override
    @Transactional
    public CommentResponseDto updateComment(UUID commentId, UUID userId, String content) {

        Comment comment = commentRepository.findByIdAndDeletedFalse(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        if (!comment.getUserId().equals(userId)) {
            throw new UnauthorizedException("Not allowed");
        }

        comment.setContent(content);

        return mapToResponse(commentRepository.save(comment));
    }

    // 🔥 GET COMMENT BY ID
    @Override
    @Transactional(readOnly = true)
    public CommentResponseDto getCommentById(UUID commentId) {

        return mapToResponse(
                commentRepository.findByIdAndDeletedFalse(commentId)
                        .orElseThrow(() -> new ResourceNotFoundException("Comment not found"))
        );
    }

    // 🔥 GET COMMENTS BY USER
    @Override
    @Transactional(readOnly = true)
    public List<CommentResponseDto> getCommentsByUser(UUID userId) {

        return commentRepository.findByUserIdAndDeletedFalse(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // 🔥 LIKE COMMENT
    @Override
    @Transactional
    public void likeComment(UUID commentId) {

        Comment comment = commentRepository.findByIdAndDeletedFalse(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        comment.setLikesCount(comment.getLikesCount() + 1);
        commentRepository.save(comment);
    }

    // 🔥 UNLIKE COMMENT
    @Override
    @Transactional
    public void unlikeComment(UUID commentId) {

        Comment comment = commentRepository.findByIdAndDeletedFalse(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        comment.setLikesCount(Math.max(0, comment.getLikesCount() - 1));
        commentRepository.save(comment);
    }

    // 🔥 MAPPER METHOD
    private CommentResponseDto mapToResponse(Comment comment) {

        int repliesCount = commentRepository
                .findByParentCommentIdAndDeletedFalse(comment.getId())
                .size();

        return CommentResponseDto.builder()
                .id(comment.getId())
                .userId(comment.getUserId())
                .postId(comment.getPostId())
                .parentCommentId(comment.getParentCommentId())
                .content(comment.getContent())
                .likesCount(comment.getLikesCount())
                .repliesCount(repliesCount)
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}