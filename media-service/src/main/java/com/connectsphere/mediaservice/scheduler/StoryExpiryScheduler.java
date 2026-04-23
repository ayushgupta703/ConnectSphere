package com.connectsphere.mediaservice.scheduler;

import com.connectsphere.mediaservice.service.StoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StoryExpiryScheduler {

    private final StoryService storyService;

    // 🔥 Runs every 5 minutes
    @Scheduled(fixedRate = 300000)
    public void expireStoriesJob() {

        log.info("Running story expiry scheduler...");

        storyService.expireStories();

        log.info("Story expiry job completed.");
    }
}