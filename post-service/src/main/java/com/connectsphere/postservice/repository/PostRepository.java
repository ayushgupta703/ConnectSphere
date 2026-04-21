package com.connectsphere.postservice.repository;

import com.connectsphere.postservice.entity.Post;
import com.connectsphere.postservice.enums.PostVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID> {

    Page<Post> findByIsDeletedFalse(Pageable pageable);

    Page<Post> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<Post> findByVisibilityAndIsDeletedFalse(PostVisibility visibility, Pageable pageable);

    Page<Post> findByContentContainingIgnoreCaseAndIsDeletedFalse(String keyword, Pageable pageable);

    long countByUserId(UUID userId);

    Page<Post> findByUserIdInAndIsDeletedFalseOrderByCreatedAtDesc(List<UUID> userIds, Pageable pageable);
}