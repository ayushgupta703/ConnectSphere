package com.connectsphere.mediaservice.controller;

import com.connectsphere.mediaservice.dto.MediaUploadRequest;
import com.connectsphere.mediaservice.entity.Media;
import com.connectsphere.mediaservice.service.MediaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    // 📸 Upload Media
    @PostMapping("/upload")
    public Media uploadMedia(@Valid @RequestBody MediaUploadRequest request) {

        Media media = Media.builder()
                .uploaderId(request.getUploaderId())
                .mediaType(request.getMediaType())
                .sizeKb(request.getSizeKb())
                .mimeType(request.getMimeType())
                .linkedPostId(request.getLinkedPostId())
                .build();

        return mediaService.uploadMedia(media);
    }

    // 📸 Get Media by Post
    @GetMapping("/post/{postId}")
    public List<Media> getMediaByPost(@PathVariable Long postId) {
        return mediaService.getMediaByPost(postId);
    }

    // 📸 Soft Delete
    @DeleteMapping("/{mediaId}")
    public void deleteMedia(@PathVariable Long mediaId) {
        mediaService.deleteMedia(mediaId);
    }
}