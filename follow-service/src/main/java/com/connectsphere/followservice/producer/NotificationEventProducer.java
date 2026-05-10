package com.connectsphere.followservice.producer;

import com.connectsphere.followservice.event.FollowNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEventProducer {

    private final RabbitTemplate rabbitTemplate;
    
    private static final String EXCHANGE = "notification.exchange";
    private static final String ROUTING_KEY = "notification.follow";

    public void publishNotificationEvent(FollowNotificationEvent event) {
        log.info("Publishing follow notification event to exchange: {} with routing key: {}", EXCHANGE, ROUTING_KEY);
        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, event);
    }
}
