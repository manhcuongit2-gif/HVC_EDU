package com.educenter.service.impl;

import com.educenter.entity.NotificationBroadcast;
import com.educenter.entity.User;
import com.educenter.repository.NotificationBroadcastRepository;
import com.educenter.repository.UserRepository;
import com.educenter.service.NotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class NotificationBroadcastDispatcher {
    private final NotificationBroadcastRepository broadcasts;
    private final UserRepository users;
    private final NotificationService notifications;

    public NotificationBroadcastDispatcher(NotificationBroadcastRepository broadcasts, UserRepository users, NotificationService notifications) {
        this.broadcasts = broadcasts; this.users = users; this.notifications = notifications;
    }

    @Scheduled(fixedDelayString = "${app.notifications.broadcast-interval-ms:30000}")
    @Transactional
    public void dispatchDue() {
        for (NotificationBroadcast broadcast : broadcasts.findTop50ByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc("PENDING", LocalDateTime.now())) {
            try {
                List<User> recipients = broadcast.getTargetRole() == null
                        ? users.findByActiveTrueOrderByFullNameAsc()
                        : users.findByRole_NameAndActiveTrueOrderByFullNameAsc(broadcast.getTargetRole());
                notifications.notifyUsers(recipients.stream().map(User::getId).toList(), "admin-broadcast-" + broadcast.getId(),
                        "GENERAL", broadcast.getTitle(), broadcast.getBody(), broadcast.getTargetPath());
                broadcast.setStatus("SENT"); broadcast.setSentAt(LocalDateTime.now()); broadcast.setLastError(null);
            } catch (Exception e) {
                broadcast.setAttemptCount(broadcast.getAttemptCount() + 1);
                broadcast.setLastError(e.getMessage() == null ? "Không thể gửi thông báo" : e.getMessage().substring(0, Math.min(500, e.getMessage().length())));
                if (broadcast.getAttemptCount() >= 5) broadcast.setStatus("FAILED");
            }
        }
    }
}
