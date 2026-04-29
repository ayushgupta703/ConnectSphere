package com.connectsphere.mediaservice.controller;

import com.connectsphere.mediaservice.entity.Media;
import com.connectsphere.mediaservice.service.MediaService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    // 📸 Upload Media (Multipart)
    @PostMapping("/upload")
    public ResponseEntity<Media> uploadMedia(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "linkedPostId", required = false) UUID linkedPostId,
            HttpServletRequest request
    ) {
        String userIdStr = (String) request.getAttribute("userId");

        if (userIdStr == null) {
            throw new RuntimeException("User not authenticated");
        }

        UUID uploaderId = UUID.fromString(userIdStr);

        return ResponseEntity.ok(
                mediaService.uploadMedia(file, uploaderId, linkedPostId)
        );
    }

    // 📸 Upload Profile Picture
    @PostMapping("/profile-picture")
    public ResponseEntity<Map<String, String>> uploadProfilePicture(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request
    ) {
        String userIdStr = (String) request.getAttribute("userId");

        if (userIdStr == null) {
            throw new RuntimeException("User not authenticated");
        }

        UUID uploaderId = UUID.fromString(userIdStr);

        Media media = mediaService.uploadProfilePicture(uploaderId, file);

        return ResponseEntity.ok(Map.of("profilePicUrl", media.getUrl()));
    }

    // 📸 Get Media by Post
    @GetMapping("/post/{postId}")
    public ResponseEntity<List<Media>> getMediaByPost(@PathVariable java.util.UUID postId) {
        return ResponseEntity.ok(mediaService.getMediaByPost(postId));
    }

    // 📸 Get Media by ID
    @GetMapping("/{mediaId}")
    public ResponseEntity<Media> getMediaById(@PathVariable java.util.UUID mediaId) {
        return ResponseEntity.ok(mediaService.getMediaById(mediaId));
    }

    // 📸 Soft Delete
    @DeleteMapping("/{mediaId}")
    public ResponseEntity<Void> deleteMedia(@PathVariable java.util.UUID mediaId) {
        mediaService.deleteMedia(mediaId);
        return ResponseEntity.noContent().build();
    }

    // 📸 Cleanup on Post Delete
    @DeleteMapping("/post/{postId}")
    public ResponseEntity<Void> deleteMediaByPost(@PathVariable java.util.UUID postId) {
        mediaService.deleteMediaByPost(postId);
        return ResponseEntity.noContent().build();
    }
}