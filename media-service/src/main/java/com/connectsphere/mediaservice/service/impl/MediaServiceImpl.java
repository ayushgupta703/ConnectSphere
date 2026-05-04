package com.connectsphere.mediaservice.service.impl;

import com.connectsphere.mediaservice.dto.StoryResponse;
import com.connectsphere.mediaservice.entity.Media;
import com.connectsphere.mediaservice.entity.MediaType;
import com.connectsphere.mediaservice.entity.Story;
import com.connectsphere.mediaservice.entity.StoryView;
import com.connectsphere.mediaservice.repository.MediaRepository;
import com.connectsphere.mediaservice.repository.StoryRepository;
import com.connectsphere.mediaservice.repository.StoryViewRepository;
import com.connectsphere.mediaservice.service.MediaService;
import com.connectsphere.mediaservice.util.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaServiceImpl implements MediaService {

    private final MediaRepository mediaRepository;
    private final StoryRepository storyRepository;
    private final StoryViewRepository storyViewRepository;
    private final FileUtil fileUtil;

    private static final String BASE_URL = "http://localhost:8087/uploads/";
    private static final long MAX_IMAGE_SIZE = 50 * 1024 * 1024; // 50MB
    private static final long MAX_VIDEO_SIZE = 200 * 1024 * 1024; // 200MB

    @Override
    @Transactional
    public Media uploadMedia(MultipartFile file, UUID uploaderId, UUID linkedPostId) {
        try {
            validateFileType(file);
            String filename = fileUtil.save(file);
            String url = BASE_URL + filename;

            Media media = Media.builder()
                    .uploaderId(uploaderId)
                    .url(url)
                    .mediaType(determineMediaType(file))
                    .sizeKb(file.getSize() / 1024)
                    .mimeType(file.getContentType())
                    .linkedPostId(linkedPostId)
                    .build();

            log.info("Media uploaded successfully for user {}: {}", uploaderId, filename);
            return mediaRepository.save(media);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to upload media for user {}: {}", uploaderId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Media upload failed");
        }
    }

    @Override
    @Transactional
    public Media uploadProfilePicture(UUID uploaderId, MultipartFile file) {
        try {
            validateFileType(file);
            if (determineMediaType(file) != MediaType.IMAGE) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Profile picture must be an image");
            }
            
            String filename = fileUtil.save(file);
            String url = BASE_URL + filename;

            Media media = Media.builder()
                    .uploaderId(uploaderId)
                    .url(url)
                    .mediaType(MediaType.IMAGE)
                    .sizeKb(file.getSize() / 1024)
                    .mimeType(file.getContentType())
                    .build();

            log.info("Profile picture uploaded successfully for user {}: {}", uploaderId, filename);
            return mediaRepository.save(media);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to upload profile picture for user {}: {}", uploaderId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Profile picture upload failed");
        }
    }

    @Override
    public List<Media> getMediaByPost(UUID postId) {
        return mediaRepository.findByLinkedPostIdAndIsDeletedFalse(postId);
    }

    @Override
    public Media getMediaById(UUID mediaId) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found with id: " + mediaId));
        
        if (media.isDeleted()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Media is deleted");
        }
        
        return media;
    }

    @Override
    @Transactional
    public void deleteMedia(UUID mediaId) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found with id: " + mediaId));
        media.setDeleted(true);
        mediaRepository.save(media);
    }

    @Override
    @Transactional
    public void deleteMediaByPost(UUID postId) {
        List<Media> mediaList = mediaRepository.findByLinkedPostIdAndIsDeletedFalse(postId);
        if (!mediaList.isEmpty()) {
            mediaList.forEach(m -> m.setDeleted(true));
            mediaRepository.saveAll(mediaList);
            log.info("Cleaned up {} media files for post {}", mediaList.size(), postId);
        }
    }

    @Override
    @Transactional
    public StoryResponse createStory(MultipartFile file, UUID authorId, String caption) {
        try {
            validateFileType(file);
            String filename = fileUtil.save(file);
            String url = BASE_URL + filename;

            Story story = Story.builder()
                    .authorId(authorId)
                    .mediaUrl(url)
                    .caption(caption)
                    .mediaType(determineMediaType(file))
                    .build();

            log.info("Story created successfully for user {}: {}", authorId, filename);
            Story saved = storyRepository.save(story);
            return mapToResponse(saved);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create story for user {}: {}", authorId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Story creation failed");
        }
    }

    @Override
    public List<StoryResponse> getActiveStories() {
        return storyRepository.findByIsActiveTrueAndExpiresAtAfter(LocalDateTime.now())
                .stream().map(this::mapToResponse).toList();
    }

    @Override
    public List<StoryResponse> getStoriesByUser(UUID userId) {
        return storyRepository.findByAuthorIdAndIsActiveTrue(userId).stream()
                .filter(s -> s.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(this::mapToResponse).toList();
    }

    @Override
    @Transactional
    public void viewStory(UUID storyId, UUID viewerId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Story not found with id: " + storyId));

        // 🚫 1. Prevent self-view
        if (story.getAuthorId().equals(viewerId)) {
            log.info("Story owner {} tried to view their own story {}", viewerId, storyId);
            return;
        }

        // 🚫 2. Check if already viewed
        boolean alreadyViewed = storyViewRepository.existsByStoryIdAndViewerId(storyId, viewerId);
        if (alreadyViewed) {
            return;
        }

        // ✅ 3. Save new view
        StoryView view = StoryView.builder()
                .storyId(storyId)
                .viewerId(viewerId)
                .viewedAt(LocalDateTime.now())
                .build();
        storyViewRepository.save(view);

        // ✅ 4. Increment safely
        Long currentViews = story.getViewsCount() == null ? 0L : story.getViewsCount();
        story.setViewsCount(currentViews + 1);

        storyRepository.save(story);
        log.info("Unique view registered for story {} by user {}", storyId, viewerId);
    }

    @Override
    @Transactional
    public void deleteStory(UUID storyId, UUID authorId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Story not found with id: " + storyId));

        if (!story.getAuthorId().equals(authorId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You are not authorized to delete this story");
        }

        story.setActive(false);
        storyRepository.save(story);
        log.info("Story {} deleted by author {}", storyId, authorId);
    }


    @Override
    @Transactional
    public void expireOldStories() {
        int count = storyRepository.deactivateExpiredStories(LocalDateTime.now());
        if (count > 0) {
            log.info("Expired {} stories automatically", count);
        }
    }

    private void validateFileType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file type");
        }
        
        MediaType type = determineMediaType(file);
        long size = file.getSize();
        
        if (type == MediaType.IMAGE && size > MAX_IMAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image size exceeds 5MB limit");
        }
        
        if (type == MediaType.VIDEO && size > MAX_VIDEO_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Video size exceeds 20MB limit");
        }

        if (!(contentType.startsWith("image/") || contentType.equals("video/mp4"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only IMAGE (jpg, png, webp) and VIDEO (mp4) are allowed");
        }
        
        if (contentType.startsWith("image/") && 
            !(contentType.endsWith("jpeg") || contentType.endsWith("png") || contentType.endsWith("webp"))) {
             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only jpg, png, webp images are allowed");
        }
    }

    private MediaType determineMediaType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && contentType.startsWith("video/")) {
            return MediaType.VIDEO;
        }
        return MediaType.IMAGE;
    }

    private StoryResponse mapToResponse(Story story) {
        return StoryResponse.builder()
                .id(story.getStoryId())
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