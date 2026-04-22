package com.connectsphere.followservice.repository;

import com.connectsphere.followservice.entity.Follow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FollowRepository extends JpaRepository<Follow, UUID> {

    boolean existsByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    Optional<Follow> findByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    void deleteByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    Page<Follow> findByFollowerId(UUID followerId, Pageable pageable);

    Page<Follow> findByFollowingId(UUID followingId, Pageable pageable);
}