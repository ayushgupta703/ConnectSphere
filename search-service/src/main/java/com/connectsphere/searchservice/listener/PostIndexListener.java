package com.connectsphere.searchservice.listener;

import com.connectsphere.searchservice.config.RabbitMQConfig;
import com.connectsphere.searchservice.event.PostIndexEvent;
import com.connectsphere.searchservice.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PostIndexListener {
    private final SearchService searchService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void handlePostIndex(PostIndexEvent event) {
        log.info("Received PostIndexEvent for postId: {}", event.getPostId());
        searchService.indexPost(event.getPostId(), event.getContent());
    }
}
