package com.connectsphere.mediaservice.scheduler;

import com.connectsphere.mediaservice.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StoryExpiryScheduler {

    private final MediaService mediaService;

    @Scheduled(fixedRate = 300000) // every 5 min
    public void expireOldStories() {
        mediaService.expireOldStories();
    }
}