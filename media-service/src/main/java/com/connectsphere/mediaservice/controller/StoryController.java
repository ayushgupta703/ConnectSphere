package com.connectsphere.mediaservice.controller;

import com.connectsphere.mediaservice.dto.StoryRequest;
import com.connectsphere.mediaservice.dto.StoryResponse;
import com.connectsphere.mediaservice.entity.Story;
import com.connectsphere.mediaservice.service.StoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stories")
@RequiredArgsConstructor
public class StoryController {

    private final StoryService storyService;

    // 📖 Create Story
    @PostMapping
    public StoryResponse createStory(@Valid @RequestBody StoryRequest request) {

        Story story = Story.builder()
                .authorId(request.getAuthorId())
                .mediaUrl(request.getMediaUrl())
                .caption(request.getCaption())
                .mediaType(request.getMediaType())
                .build();

        Story saved = storyService.createStory(story);

        return mapToResponse(saved);
    }

    // 📖 Story Feed (FOLLOW BASED)
    @GetMapping("/feed")
    public List<Story> getStoryFeed(
            @RequestHeader("Authorization") String token,
            @RequestParam Long userId
    ) {
        return storyService.getStoryFeed(userId, token);
    }

    // 📖 View Story (increment)
    @PostMapping("/{storyId}/view")
    public void viewStory(@PathVariable Long storyId) {
        storyService.viewStory(storyId);
    }

    // 📖 Get User Stories
    @GetMapping("/user/{userId}")
    public List<Story> getStoriesByUser(@PathVariable Long userId) {
        return storyService.getStoriesByUser(userId);
    }

    // 🔁 Mapper
    private StoryResponse mapToResponse(Story story) {
        return StoryResponse.builder()
                .id(story.getId())
                .authorId(story.getAuthorId())
                .mediaUrl(story.getMediaUrl())
                .caption(story.getCaption())
                .mediaType(story.getMediaType())
                .viewsCount(story.getViewsCount())
                .createdAt(story.getCreatedAt())
                .expiresAt(story.getExpiresAt())
                .build();
    }
}