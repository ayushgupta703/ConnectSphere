package com.connectsphere.postservice.repository;

import com.connectsphere.postservice.entity.Post;
import com.connectsphere.postservice.enums.PostVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID> {

    // 🔹 Get all non-deleted posts (pagination)
    Page<Post> findByIsDeletedFalse(Pageable pageable);

    // 🔹 Get posts by user (non-deleted, latest first)
    Page<Post> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    // 🔹 Get posts by visibility (for future use)
    Page<Post> findByVisibilityAndIsDeletedFalse(PostVisibility visibility, Pageable pageable);

}