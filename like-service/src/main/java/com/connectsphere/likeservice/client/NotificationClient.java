package com.connectsphere.likeservice.client;

import com.connectsphere.likeservice.dto.NotificationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "notification-service", url = "http://localhost:8085")
public interface NotificationClient {

    @PostMapping("/api/v1/notifications")
    void sendNotification(@RequestBody NotificationRequest request,
                          @RequestHeader("Authorization") String token);
}