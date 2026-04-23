package com.connectsphere.mediaservice.service.impl;

import com.connectsphere.mediaservice.entity.Media;
import com.connectsphere.mediaservice.repository.MediaRepository;
import com.connectsphere.mediaservice.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements MediaService {

    private final MediaRepository mediaRepository;

    @Override
    public Media uploadMedia(Media media) {

        // 🔥 Simulate CDN URL (important)
        media.setUrl("https://cdn.connectsphere.com/" + System.currentTimeMillis());

        return mediaRepository.save(media);
    }

    @Override
    public List<Media> getMediaByPost(Long postId) {
        return mediaRepository.findByLinkedPostIdAndIsDeletedFalse(postId);
    }

    @Override
    public void deleteMedia(Long mediaId) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new RuntimeException("Media not found with id: " + mediaId));

        media.setDeleted(true);
        mediaRepository.save(media);
    }
}