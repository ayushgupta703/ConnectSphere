package com.connectsphere.notificationservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "notification.exchange";
    public static final String LIKE_QUEUE_NAME = "notification.like.queue";
    public static final String LIKE_ROUTING_KEY = "notification.like";

    public static final String COMMENT_QUEUE_NAME = "notification.comment.queue";
    public static final String COMMENT_ROUTING_KEY = "notification.comment";

    public static final String FOLLOW_QUEUE_NAME = "notification.follow.queue";
    public static final String FOLLOW_ROUTING_KEY = "notification.follow";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue likeQueue() {
        return new Queue(LIKE_QUEUE_NAME, true); // Durable queue
    }

    @Bean
    public Binding likeBinding(Queue likeQueue, TopicExchange exchange) {
        return BindingBuilder.bind(likeQueue).to(exchange).with(LIKE_ROUTING_KEY);
    }

    @Bean
    public Queue commentQueue() {
        return new Queue(COMMENT_QUEUE_NAME, true);
    }

    @Bean
    public Binding commentBinding(Queue commentQueue, TopicExchange exchange) {
        return BindingBuilder.bind(commentQueue).to(exchange).with(COMMENT_ROUTING_KEY);
    }

    @Bean
    public Queue followQueue() {
        return new Queue(FOLLOW_QUEUE_NAME, true);
    }

    @Bean
    public Binding followBinding(Queue followQueue, TopicExchange exchange) {
        return BindingBuilder.bind(followQueue).to(exchange).with(FOLLOW_ROUTING_KEY);
    }
}
