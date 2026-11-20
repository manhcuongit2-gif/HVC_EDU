package com.educenter.controller;

import com.educenter.security.CustomUserDetails;
import com.educenter.service.NotificationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalTime;

@Controller
@RequestMapping("/notifications")
public class NotificationPageController {
    private final NotificationService notifications;
    public NotificationPageController(NotificationService notifications) { this.notifications = notifications; }

    @GetMapping
    public String index(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        model.addAttribute("currentUser", user);
        model.addAttribute("notifications", notifications.latest(user.getId()));
        model.addAttribute("unreadCount", notifications.unreadCount(user.getId()));
        return "notifications/index";
    }
    @GetMapping("/settings")
    public String settings(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        model.addAttribute("currentUser", user);
        model.addAttribute("preference", notifications.preference(user.getId()));
        return "notifications/settings";
    }

    @PostMapping("/settings")
    public String saveSettings(@AuthenticationPrincipal CustomUserDetails user,
                               @RequestParam(defaultValue = "false") boolean pushEnabled,
                               @RequestParam(defaultValue = "false") boolean privateMessages,
                               @RequestParam(defaultValue = "false") boolean groupMessages,
                               @RequestParam(defaultValue = "false") boolean tuition,
                               @RequestParam(defaultValue = "false") boolean tuitionReminders,
                               @RequestParam(defaultValue = "false") boolean scheduleReminders,
                               @RequestParam(defaultValue = "false") boolean scheduleChanges,
                               @RequestParam(defaultValue = "false") boolean attendance,
                               @RequestParam(defaultValue = "false") boolean learningReports,
                               @RequestParam(defaultValue = "false") boolean homework,
                               @RequestParam(defaultValue = "false") boolean general,
                               @RequestParam(defaultValue = "false") boolean soundEnabled,
                               @RequestParam(defaultValue = "false") boolean previewEnabled,
                               @RequestParam(defaultValue = "false") boolean quietHoursEnabled,
                               @RequestParam(required = false) LocalTime quietStart,
                               @RequestParam(required = false) LocalTime quietEnd,
                               RedirectAttributes redirect) {
        if (quietHoursEnabled && (quietStart == null || quietEnd == null || quietStart.equals(quietEnd))) {
            redirect.addFlashAttribute("errorMessage", "Chọn giờ bắt đầu và kết thúc khác nhau cho khung giờ yên lặng.");
            return "redirect:/notifications/settings";
        }
        notifications.updatePreference(user.getId(), pushEnabled, privateMessages, groupMessages, tuition, tuitionReminders,
                scheduleReminders, scheduleChanges, attendance, learningReports, homework, general,
                soundEnabled, previewEnabled, quietHoursEnabled, quietStart, quietEnd);
        redirect.addFlashAttribute("successMessage", "Đã lưu tùy chọn thông báo.");
        return "redirect:/notifications/settings";
    }
    @GetMapping("/devices")
    public String devices(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        model.addAttribute("currentUser", user);
        model.addAttribute("pushEnabled", notifications.subscriptions(user.getId()).size());
        return "notifications/devices";
    }
}
