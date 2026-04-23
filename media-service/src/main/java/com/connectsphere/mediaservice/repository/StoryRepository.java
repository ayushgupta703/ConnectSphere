package com.connectsphere.mediaservice.repository;

import com.connectsphere.mediaservice.entity.Story;
import org.springframework.data.jpa.repository.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface StoryRepository extends JpaRepository<Story, Long> {

    // 🔹 Get active stories of a specific user
    List<Story> findByAuthorIdAndIsActiveTrue(Long authorId);

    // 🔥 Feed query (IMPORTANT)
    @Query("""
        SELECT s FROM Story s
        WHERE s.authorId IN :followingIds
        AND s.isActive = true
        AND s.expiresAt > :now
    """)
    List<Story> findActiveStoriesForFeed(List<Long> followingIds, LocalDateTime now);

    // 🔥 Bulk expiry update (IMPORTANT)
    @Modifying
    @Transactional
    @Query("""
        UPDATE Story s
        SET s.isActive = false
        WHERE s.expiresAt <= :now
        AND s.isActive = true
    """)
    int deactivateExpiredStories(LocalDateTime now);
}