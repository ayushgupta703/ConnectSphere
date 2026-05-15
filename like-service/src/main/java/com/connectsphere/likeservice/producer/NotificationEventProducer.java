package com.connectsphere.likeservice.producer;

import com.connectsphere.likeservice.config.RabbitMQConfig;
import com.connectsphere.likeservice.event.LikeNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventProducer {
    private final RabbitTemplate rabbitTemplate;

    public void publishNotificationEvent(LikeNotificationEvent event) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY, event);
        log.info("Published LikeNotificationEvent for targetId={}", event.getTargetId());
    }
}
