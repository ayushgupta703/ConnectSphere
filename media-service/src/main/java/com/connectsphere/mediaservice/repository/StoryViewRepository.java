package com.connectsphere.mediaservice.repository;

import com.connectsphere.mediaservice.entity.StoryView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StoryViewRepository extends JpaRepository<StoryView, Long> {
    boolean existsByStoryIdAndViewerId(UUID storyId, UUID viewerId);
}
