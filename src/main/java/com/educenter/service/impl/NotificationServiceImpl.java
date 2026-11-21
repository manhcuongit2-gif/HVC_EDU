package com.educenter.service.impl;

import com.educenter.entity.*;
import com.educenter.repository.*;
import com.educenter.service.PushSubscriptionOwnershipException;
import com.educenter.service.NotificationService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HexFormat;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {
    private static final int MAX_TITLE = 180;
    private static final int MAX_BODY = 500;
    private final UserRepository users;
    private final UserNotificationRepository notifications;
    private final NotificationOutboxRepository outbox;
    private final NotificationPreferenceRepository preferences;
    private final PushSubscriptionRepository subscriptions;
    private final PushSubscriptionUserRepository subscriptionUsers;

    public NotificationServiceImpl(UserRepository users, UserNotificationRepository notifications,
                                   NotificationOutboxRepository outbox, NotificationPreferenceRepository preferences,
                                   PushSubscriptionRepository subscriptions, PushSubscriptionUserRepository subscriptionUsers) {
        this.users = users;
        this.notifications = notifications;
        this.outbox = outbox;
        this.preferences = preferences;
        this.subscriptions = subscriptions;
        this.subscriptionUsers = subscriptionUsers;
    }

    @Override
    @Transactional
    public UserNotification notifyUser(Long userId, String eventKey, String category, String title, String body, String targetPath) {
        String stableKey = safe(eventKey, 191);
        if (notifications.existsByUserIdAndEventKey(userId, stableKey)) {
            return notifications.findByUserIdAndEventKey(userId, stableKey).orElse(null);
        }
        User user = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản nhận thông báo."));
        UserNotification item = new UserNotification();
        item.setUser(user);
        item.setEventKey(stableKey);
        item.setCategory(normalizeCategory(category));
        item.setTitle(safe(title, MAX_TITLE));
        item.setBody(safe(body, MAX_BODY));
        item.setTargetPath(safePath(targetPath));
        item = notifications.save(item);

        NotificationPreference preference = getOrCreatePreference(user);
        if (preference.isPushEnabled() && categoryEnabled(preference, item.getCategory())) {
            String pushKey = safe(stableKey + ":" + userId, 191);
            if (!outbox.existsByEventKey(pushKey)) {
                NotificationOutbox job = new NotificationOutbox();
                job.setUser(user);
                job.setNotification(item);
                job.setEventKey(pushKey);
                job.setCategory(item.getCategory());
                // Push is always generic because the operating system may show it on a lock screen.
                job.setTitle(PushNotificationContent.title(item.getCategory()));
                job.setBody(PushNotificationContent.body(item.getCategory()));
                job.setSoundEnabled(preference.isSoundEnabled());
                job.setTargetPath(item.getTargetPath());
                job.setScheduledAt(quietEndIfNeeded(preference, LocalDateTime.now()));
                outbox.save(job);
            }
        }
        return item;
    }

    @Override
    @Transactional
    public void notifyUsers(Iterable<Long> userIds, String eventKey, String category, String title, String body, String targetPath) {
        if (userIds == null) return;
        for (Long userId : userIds) if (userId != null) notifyUser(userId, eventKey, category, title, body, targetPath);
    }

    @Override @Transactional(readOnly = true)
    public List<UserNotification> latest(Long userId) {
        return notifications.findByUserIdAndHiddenAtIsNullOrderByCreatedAtDesc(userId,
                org.springframework.data.domain.PageRequest.of(0, 100));
    }

    @Override @Transactional(readOnly = true)
    public long unreadCount(Long userId) { return notifications.countByUserIdAndReadAtIsNullAndHiddenAtIsNull(userId); }

    @Override @Transactional
    public void markRead(Long userId, Long notificationId) {
        UserNotification item = ownedNotification(userId, notificationId);
        if (item.getReadAt() == null) item.setReadAt(LocalDateTime.now());
    }

    @Override @Transactional
    public void markAllRead(Long userId) {
        notifications.markAllRead(userId, LocalDateTime.now());
    }

    @Override @Transactional
    public void hide(Long userId, Long notificationId) { ownedNotification(userId, notificationId).setHiddenAt(LocalDateTime.now()); }

    @Override @Transactional(readOnly = true)
    public List<PushSubscriptionUser> subscriptions(Long userId) {
        return subscriptionUsers.findByUserIdAndActiveTrueAndSubscriptionActiveTrueOrderBySubscriptionLastSeenAtDesc(userId);
    }

    @Override @Transactional
    public void saveSubscription(Long userId, String endpoint, String p256dh, String auth, String deviceName, String userAgent, String platform) {
        if (endpoint == null || endpoint.isBlank() || endpoint.length() > 2048
                || p256dh == null || p256dh.isBlank() || p256dh.length() > 256
                || auth == null || auth.isBlank() || auth.length() > 128)
            throw new IllegalArgumentException("Thông tin thiết bị nhận thông báo không hợp lệ.");
        validateEndpoint(endpoint);
        User user = users.findById(userId).orElseThrow();
        String hash = sha256(endpoint);
        PushSubscription subscription = subscriptions.findByEndpointHash(hash).orElse(null);
        try {
            if (subscription == null) {
                subscription = new PushSubscription();
                subscription.setUser(user);
                subscription.setEndpoint(endpoint);
                subscription.setEndpointHash(hash);
                subscription.setP256dhKey(p256dh);
                subscription.setAuthKey(auth);
                subscription.setDeviceName(safe(deviceName == null ? "Thiết bị của tôi" : deviceName, 100));
                subscription.setUserAgent(safe(userAgent, 512));
                subscription.setPlatform(safe(platform, 80));
                subscription.setActive(true);
                subscription = subscriptions.saveAndFlush(subscription);
            } else {
                // The physical endpoint and its registering account are immutable. Another
                // signed-in account may add its own opt-in link without taking ownership.
                if (!endpoint.equals(subscription.getEndpoint())
                        || !p256dh.equals(subscription.getP256dhKey())
                        || !auth.equals(subscription.getAuthKey())) {
                    throw new PushSubscriptionOwnershipException();
                }
                subscription.setActive(true);
            }
            PushSubscription linkedSubscription = subscription;
            PushSubscriptionUser link = subscriptionUsers.findBySubscriptionIdAndUserId(linkedSubscription.getId(), userId)
                    .orElseGet(() -> {
                        PushSubscriptionUser created = new PushSubscriptionUser();
                        created.setSubscription(linkedSubscription);
                        created.setUser(user);
                        return created;
                    });
            link.setActive(true);
            subscriptionUsers.saveAndFlush(link);
        } catch (DataIntegrityViolationException ex) {
            // A concurrent registration may have created the endpoint or account link after lookup.
            throw new PushSubscriptionOwnershipException();
        }
        getOrCreatePreference(user);
    }

    @Override @Transactional
    public void removeSubscription(Long userId, Long subscriptionId) {
        PushSubscriptionUser link = subscriptionUsers.findByIdAndUserIdAndActiveTrue(subscriptionId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thiết bị."));
        link.setActive(false);
    }

    @Override @Transactional
    public NotificationPreference preference(Long userId) {
        User user = users.findById(userId).orElseThrow();
        return getOrCreatePreference(user);
    }

    @Override @Transactional
    public void updatePreference(Long userId, boolean pushEnabled, boolean privateMessages, boolean groupMessages,
                                 boolean tuition, boolean tuitionReminders, boolean scheduleReminders, boolean scheduleChanges,
                                 boolean attendance, boolean learningReports, boolean homework, boolean general,
                                 boolean soundEnabled, boolean previewEnabled, boolean quietHoursEnabled,
                                 LocalTime quietStart, LocalTime quietEnd) {
        NotificationPreference p = preference(userId);
        p.setPushEnabled(pushEnabled); p.setPrivateMessages(privateMessages); p.setGroupMessages(groupMessages);
        p.setTuition(tuition); p.setTuitionReminders(tuitionReminders); p.setScheduleReminders(scheduleReminders);
        p.setScheduleChanges(scheduleChanges); p.setAttendance(attendance); p.setLearningReports(learningReports);
        p.setHomework(homework); p.setGeneral(general); p.setSoundEnabled(soundEnabled);
        // Kept in the schema/API for compatibility, but detailed lock-screen previews are disabled.
        p.setPreviewEnabled(false);
        p.setQuietHoursEnabled(quietHoursEnabled); p.setQuietStart(quietStart); p.setQuietEnd(quietEnd);
    }

    private UserNotification ownedNotification(Long userId, Long id) {
        return notifications.findByIdAndUserId(id, userId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông báo."));
    }
    private NotificationPreference getOrCreatePreference(User user) {
        return preferences.findByUserId(user.getId()).orElseGet(() -> {
            NotificationPreference p = new NotificationPreference(); p.setUser(user); return preferences.save(p);
        });
    }
    private boolean categoryEnabled(NotificationPreference p, String category) {
        return switch (category) {
            case "MESSAGE_PRIVATE" -> p.isPrivateMessages();
            case "MESSAGE_GROUP" -> p.isGroupMessages();
            case "TUITION" -> p.isTuition();
            case "TUITION_REMINDER" -> p.isTuitionReminders();
            case "SCHEDULE_REMINDER" -> p.isScheduleReminders();
            case "SCHEDULE_CHANGE" -> p.isScheduleChanges();
            case "ATTENDANCE" -> p.isAttendance();
            case "LEARNING_REPORT" -> p.isLearningReports();
            case "HOMEWORK" -> p.isHomework();
            default -> p.isGeneral();
        };
    }
    private LocalDateTime quietEndIfNeeded(NotificationPreference p, LocalDateTime now) {
        if (!p.isQuietHoursEnabled() || p.getQuietStart() == null || p.getQuietEnd() == null || p.getQuietStart().equals(p.getQuietEnd())) return now;
        LocalTime time = now.toLocalTime();
        boolean quiet = p.getQuietStart().isBefore(p.getQuietEnd())
                ? !time.isBefore(p.getQuietStart()) && time.isBefore(p.getQuietEnd())
                : !time.isBefore(p.getQuietStart()) || time.isBefore(p.getQuietEnd());
        if (!quiet) return now;
        LocalDateTime end = now.toLocalDate().atTime(p.getQuietEnd());
        if (!p.getQuietStart().isBefore(p.getQuietEnd()) && !time.isBefore(p.getQuietStart())) end = end.plusDays(1);
        return end;
    }
    private String normalizeCategory(String category) { return category == null || !category.matches("[A-Z_]{1,40}") ? "GENERAL" : category; }
    private String safe(String value, int max) { if (value == null) return ""; String s = value.trim(); return s.length() <= max ? s : s.substring(0, max); }
    private String safePath(String path) {
        if (path == null || path.length() > 2048 || !path.startsWith("/") || path.startsWith("//")
                || path.indexOf('\\') >= 0 || path.chars().anyMatch(Character::isISOControl)) return "/notifications";
        return path;
    }
    private String sha256(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
    private void validateEndpoint(String endpoint) {
        try {
            URI uri = URI.create(endpoint);
            String host = uri.getHost();
            boolean trusted = host != null && (host.equals("fcm.googleapis.com") || host.endsWith(".push.services.mozilla.com")
                    || host.equals("web.push.apple.com") || host.endsWith(".push.apple.com"));
            if (!"https".equalsIgnoreCase(uri.getScheme()) || !trusted) throw new IllegalArgumentException("Nhà cung cấp push không hợp lệ.");
        } catch (RuntimeException e) { throw new IllegalArgumentException("Địa chỉ push không hợp lệ."); }
    }
}
