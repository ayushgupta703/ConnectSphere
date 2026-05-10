package com.connectsphere.commentservice.producer;

import com.connectsphere.commentservice.event.CommentNotificationEvent;
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
    private static final String ROUTING_KEY = "notification.comment";

    public void publishNotificationEvent(CommentNotificationEvent event) {
        log.info("Publishing comment notification event to exchange: {} with routing key: {}", EXCHANGE, ROUTING_KEY);
        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, event);
    }
}
