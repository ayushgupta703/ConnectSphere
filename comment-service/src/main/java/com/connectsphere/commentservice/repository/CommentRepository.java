package com.connectsphere.commentservice.repository;

import com.connectsphere.commentservice.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    // ✅ TOP-LEVEL COMMENTS (IMPORTANT)
    Page<Comment> findByPostIdAndParentCommentIdIsNullAndDeletedFalse(UUID postId, Pageable pageable);

    // ✅ REPLIES
    List<Comment> findByParentCommentIdAndDeletedFalse(UUID parentCommentId);

    // ✅ COUNT COMMENTS
    long countByPostIdAndDeletedFalse(UUID postId);

    // ✅ GET COMMENT BY ID (SAFE)
    Optional<Comment> findByIdAndDeletedFalse(UUID id);

    // ✅ GET COMMENTS BY USER
    List<Comment> findByUserIdAndDeletedFalse(UUID userId);
}