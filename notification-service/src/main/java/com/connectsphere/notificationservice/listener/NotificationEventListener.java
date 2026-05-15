package com.connectsphere.notificationservice.listener;

import com.connectsphere.notificationservice.config.RabbitMQConfig;
import com.connectsphere.notificationservice.dto.NotificationRequest;
import com.connectsphere.notificationservice.event.CommentNotificationEvent;
import com.connectsphere.notificationservice.event.FollowNotificationEvent;
import com.connectsphere.notificationservice.event.LikeNotificationEvent;
import com.connectsphere.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.LIKE_QUEUE_NAME)
    public void handleLikeNotificationEvent(LikeNotificationEvent event) {
        log.info("Received LikeNotificationEvent for recipient: {}, actor: {}, target: {}",
                event.getRecipientId(), event.getActorId(), event.getTargetId());

        try {
            NotificationRequest request = NotificationRequest.builder()
                    .recipientId(event.getRecipientId())
                    .actorId(event.getActorId())
                    .type(event.getType())
                    .targetId(event.getTargetId())
                    .build();

            notificationService.createNotification(request);
            log.info("Successfully created notification for recipient: {}", event.getRecipientId());
        } catch (Exception e) {
            log.error("Failed to process LikeNotificationEvent for recipient: {}. Error: {}", event.getRecipientId(), e.getMessage(), e);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.COMMENT_QUEUE_NAME)
    public void handleCommentNotificationEvent(CommentNotificationEvent event) {
        log.info("Received CommentNotificationEvent for recipient: {}, actor: {}, target: {}",
                event.getRecipientId(), event.getActorId(), event.getTargetId());

        try {
            NotificationRequest request = NotificationRequest.builder()
                    .recipientId(event.getRecipientId())
                    .actorId(event.getActorId())
                    .type(event.getType())
                    .targetId(event.getTargetId())
                    .build();

            notificationService.createNotification(request);
            log.info("Successfully created notification for recipient: {}", event.getRecipientId());
        } catch (Exception e) {
            log.error("Failed to process CommentNotificationEvent for recipient: {}. Error: {}", event.getRecipientId(), e.getMessage(), e);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.FOLLOW_QUEUE_NAME)
    public void handleFollowNotificationEvent(FollowNotificationEvent event) {
        log.info("Received FollowNotificationEvent for recipient: {}, actor: {}, target: {}",
                event.getRecipientId(), event.getActorId(), event.getTargetId());

        try {
            NotificationRequest request = NotificationRequest.builder()
                    .recipientId(event.getRecipientId())
                    .actorId(event.getActorId())
                    .type(event.getType())
                    .targetId(event.getTargetId())
                    .build();

            notificationService.createNotification(request);
            log.info("Successfully created notification for recipient: {}", event.getRecipientId());
        } catch (Exception e) {
            log.error("Failed to process FollowNotificationEvent for recipient: {}. Error: {}", event.getRecipientId(), e.getMessage(), e);
        }
    }
}
