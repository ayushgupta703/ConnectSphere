package com.connectsphere.mediaservice.service;

import com.connectsphere.mediaservice.entity.Story;

import java.util.List;

public interface StoryService {

    Story createStory(Story story);

    List<Story> getStoriesByUser(Long userId);

    List<Story> getStoryFeed(Long userId, String token); // will use Feign later

    void viewStory(Long storyId);

    void expireStories();
}