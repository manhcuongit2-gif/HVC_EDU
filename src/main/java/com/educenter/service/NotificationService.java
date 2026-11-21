package com.educenter.service;

import com.educenter.entity.NotificationPreference;
import com.educenter.entity.PushSubscription;
import com.educenter.entity.PushSubscriptionUser;
import com.educenter.entity.UserNotification;

import java.time.LocalTime;
import java.util.List;

public interface NotificationService {
    UserNotification notifyUser(Long userId, String eventKey, String category, String title, String body, String targetPath);
    void notifyUsers(Iterable<Long> userIds, String eventKey, String category, String title, String body, String targetPath);
    List<UserNotification> latest(Long userId);
    long unreadCount(Long userId);
    void markRead(Long userId, Long notificationId);
    void markAllRead(Long userId);
    void hide(Long userId, Long notificationId);
    List<PushSubscriptionUser> subscriptions(Long userId);
    void saveSubscription(Long userId, String endpoint, String p256dh, String auth, String deviceName, String userAgent, String platform);
    void removeSubscription(Long userId, Long subscriptionId);
    NotificationPreference preference(Long userId);
    void updatePreference(Long userId, boolean pushEnabled, boolean privateMessages, boolean groupMessages,
                          boolean tuition, boolean tuitionReminders, boolean scheduleReminders, boolean scheduleChanges,
                          boolean attendance, boolean learningReports, boolean homework, boolean general,
                          boolean soundEnabled, boolean previewEnabled, boolean quietHoursEnabled,
                          LocalTime quietStart, LocalTime quietEnd);
}
