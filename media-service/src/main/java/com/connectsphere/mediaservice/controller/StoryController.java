package com.connectsphere.mediaservice.controller;

import com.connectsphere.mediaservice.dto.StoryResponse;
import com.connectsphere.mediaservice.service.MediaService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stories")
@RequiredArgsConstructor
public class StoryController {

    private final MediaService mediaService;

    // 📖 Create Story
    @PostMapping
    public ResponseEntity<StoryResponse> createStory(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "caption", required = false) String caption,
            HttpServletRequest request) {
        String userIdStr = (String) request.getAttribute("userId");

        if (userIdStr == null) {
            throw new RuntimeException("User Not Authenticated");
        }

        UUID authorId = UUID.fromString(userIdStr);
        return ResponseEntity.ok(mediaService.createStory(file, authorId, caption));
    }

    // 📖 Get Active Stories (All)
    @GetMapping("/active")
    public ResponseEntity<List<StoryResponse>> getActiveStories() {
        return ResponseEntity.ok(mediaService.getActiveStories());
    }

    // 📖 Get User Stories
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<StoryResponse>> getStoriesByUser(@PathVariable java.util.UUID userId) {
        return ResponseEntity.ok(mediaService.getStoriesByUser(userId));
    }

    // 📖 View Story (unique)
    @PostMapping("/{storyId}/view")
    public ResponseEntity<Void> viewStory(
            @PathVariable java.util.UUID storyId,
            HttpServletRequest request) {
        String userIdStr = (String) request.getAttribute("userId");
        if (userIdStr == null) {
            throw new RuntimeException("User Not Authenticated");
        }
        UUID viewerId = UUID.fromString(userIdStr);
        mediaService.viewStory(storyId, viewerId);
        return ResponseEntity.ok().build();
    }

    // 📖 Delete Story (Soft)
    @DeleteMapping("/{storyId}")
    public ResponseEntity<Void> deleteStory(
            @PathVariable java.util.UUID storyId,
            HttpServletRequest request) {
        String userIdStr = (String) request.getAttribute("userId");
        if (userIdStr == null) {
            throw new RuntimeException("User Not Authenticated");
        }
        UUID authorId = UUID.fromString(userIdStr);

        mediaService.deleteStory(storyId, authorId);
        return ResponseEntity.noContent().build();
    }
}