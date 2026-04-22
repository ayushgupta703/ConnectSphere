package com.connectsphere.commentservice.service;

import com.connectsphere.commentservice.dto.CommentResponseDto;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface CommentService {

    CommentResponseDto addComment(UUID userId, UUID postId, String content, UUID parentCommentId);

    void deleteComment(UUID commentId, UUID userId);

    Page<CommentResponseDto> getComments(UUID postId, int page, int size);

    List<CommentResponseDto> getReplies(UUID commentId);

    long countComments(UUID postId);

    // 🔥 NEW
    CommentResponseDto updateComment(UUID commentId, UUID userId, String content);

    CommentResponseDto getCommentById(UUID commentId);

    List<CommentResponseDto> getCommentsByUser(UUID userId);

    void likeComment(UUID commentId);

    void unlikeComment(UUID commentId);
}