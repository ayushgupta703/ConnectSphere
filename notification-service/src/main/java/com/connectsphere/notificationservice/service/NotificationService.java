package com.connectsphere.notificationservice.service;

import com.connectsphere.notificationservice.dto.NotificationRequest;
import com.connectsphere.notificationservice.entity.Notification;

import java.util.List;

public interface NotificationService {

    void createNotification(NotificationRequest request);

    List<Notification> getUserNotifications(String userId);

    void markAsRead(String notificationId);

    void markAllAsRead(String userId);

    long getUnreadCount(String userId);
}