package com.educenter.dto;

import com.educenter.entity.UserNotification;

import java.time.LocalDateTime;

public record BusinessNotificationView(Long id, String category, String title, String body,
                                       String targetPath, LocalDateTime createdAt, boolean read) {
    public static BusinessNotificationView from(UserNotification notification) {
        return new BusinessNotificationView(notification.getId(), notification.getCategory(),
                notification.getTitle(), notification.getBody(), notification.getTargetPath(),
                notification.getCreatedAt(), notification.isRead());
    }
}
