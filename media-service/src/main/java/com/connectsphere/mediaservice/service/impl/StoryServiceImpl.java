package com.connectsphere.mediaservice.service.impl;

import com.connectsphere.mediaservice.client.FollowClient;
import com.connectsphere.mediaservice.entity.Story;
import com.connectsphere.mediaservice.repository.StoryRepository;
import com.connectsphere.mediaservice.service.StoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StoryServiceImpl implements StoryService {

    private final StoryRepository storyRepository;
    private final FollowClient followClient;

    @Override
    public Story createStory(Story story) {
        return storyRepository.save(story);
    }

    @Override
    public List<Story> getStoriesByUser(Long userId) {
        return storyRepository.findByAuthorIdAndIsActiveTrue(userId);
    }

    @Override
    public List<Story> getStoryFeed(Long userId, String token) {

        List<Long> followingIds =
                followClient.getFollowing(userId, token);

        if (followingIds.isEmpty()) {
            return List.of();
        }

        return storyRepository.findActiveStoriesForFeed(
                followingIds,
                LocalDateTime.now()
        );
    }

    @Override
    public void viewStory(Long storyId) {

        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new RuntimeException("Story not found with id: " + storyId));

        // 🔥 IMPORTANT: increment safely
        story.setViewsCount(story.getViewsCount() + 1);

        storyRepository.save(story);
    }

    @Override
    @Transactional
    public void expireStories() {

        int count =
                storyRepository.deactivateExpiredStories(LocalDateTime.now());

        System.out.println("Expired stories: " + count);
    }
}