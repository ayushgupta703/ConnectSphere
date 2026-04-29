package com.connectsphere.mediaservice.service;

import com.connectsphere.mediaservice.dto.StoryResponse;
import com.connectsphere.mediaservice.entity.Media;
import com.connectsphere.mediaservice.entity.Story;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface MediaService {

    // 🔹 Media Methods
    Media uploadMedia(MultipartFile file, UUID uploaderId, UUID linkedPostId);
    
    Media uploadProfilePicture(UUID uploaderId, MultipartFile file);

    List<Media> getMediaByPost(UUID postId);

    Media getMediaById(UUID mediaId);

    void deleteMedia(UUID mediaId);

    void deleteMediaByPost(UUID postId);

    // 🔹 Story Methods
    StoryResponse createStory(MultipartFile file, UUID authorId, String caption);

    List<StoryResponse> getActiveStories();

    List<StoryResponse> getStoriesByUser(UUID userId);

    void viewStory(UUID storyId, UUID viewerId);

    void deleteStory(UUID storyId, UUID authorId);


    void expireOldStories();
}