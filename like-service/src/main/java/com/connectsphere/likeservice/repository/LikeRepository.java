package com.connectsphere.likeservice.repository;

import com.connectsphere.likeservice.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface LikeRepository extends JpaRepository<Like, UUID> {

    Optional<Like> findByUserIdAndPostId(UUID userId, UUID postId);

    boolean existsByUserIdAndPostId(UUID userId, UUID postId);

    void deleteByUserIdAndPostId(UUID userId, UUID postId);

    long countByPostId(UUID postId);

    List<Like> findByPostId(UUID postId);
}