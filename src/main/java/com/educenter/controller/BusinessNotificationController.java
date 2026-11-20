package com.educenter.controller;

import com.educenter.dto.BusinessNotificationView;
import com.educenter.dto.NotificationPreferenceRequest;
import com.educenter.security.CustomUserDetails;
import com.educenter.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/business-notifications")
public class BusinessNotificationController {
    private final NotificationService notifications;

    public BusinessNotificationController(NotificationService notifications) { this.notifications = notifications; }

    @GetMapping
    public Map<String, Object> list(@AuthenticationPrincipal CustomUserDetails user) {
        return Map.of("unreadCount", notifications.unreadCount(user.getId()),
                "items", notifications.latest(user.getId()).stream().map(BusinessNotificationView::from).toList());
    }

    @PostMapping("/{id}/read") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void read(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id) { notifications.markRead(user.getId(), id); }

    @PostMapping("/read-all") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void readAll(@AuthenticationPrincipal CustomUserDetails user) { notifications.markAllRead(user.getId()); }

    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void hide(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id) { notifications.hide(user.getId(), id); }

    @GetMapping("/preferences")
    public Object preference(@AuthenticationPrincipal CustomUserDetails user) {
        var p = notifications.preference(user.getId());
        return Map.ofEntries(Map.entry("pushEnabled", p.isPushEnabled()), Map.entry("privateMessages", p.isPrivateMessages()),
                Map.entry("groupMessages", p.isGroupMessages()), Map.entry("tuition", p.isTuition()),
                Map.entry("tuitionReminders", p.isTuitionReminders()), Map.entry("scheduleReminders", p.isScheduleReminders()),
                Map.entry("scheduleChanges", p.isScheduleChanges()), Map.entry("attendance", p.isAttendance()),
                Map.entry("learningReports", p.isLearningReports()), Map.entry("homework", p.isHomework()),
                Map.entry("general", p.isGeneral()), Map.entry("soundEnabled", p.isSoundEnabled()),
                Map.entry("previewEnabled", p.isPreviewEnabled()), Map.entry("quietHoursEnabled", p.isQuietHoursEnabled()),
                Map.entry("quietStart", p.getQuietStart() == null ? "" : p.getQuietStart().toString()),
                Map.entry("quietEnd", p.getQuietEnd() == null ? "" : p.getQuietEnd().toString()));
    }

    @PutMapping("/preferences") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updatePreference(@AuthenticationPrincipal CustomUserDetails user, @RequestBody NotificationPreferenceRequest p) {
        notifications.updatePreference(user.getId(), p.pushEnabled(), p.privateMessages(), p.groupMessages(), p.tuition(),
                p.tuitionReminders(), p.scheduleReminders(), p.scheduleChanges(), p.attendance(), p.learningReports(),
                p.homework(), p.general(), p.soundEnabled(), p.previewEnabled(), p.quietHoursEnabled(), p.quietStart(), p.quietEnd());
    }
}
