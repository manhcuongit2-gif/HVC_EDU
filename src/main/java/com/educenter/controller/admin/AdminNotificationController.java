package com.educenter.controller.admin;

import com.educenter.entity.NotificationBroadcast;
import com.educenter.repository.NotificationBroadcastRepository;
import com.educenter.repository.UserRepository;
import com.educenter.security.CustomUserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.time.LocalDateTime;
import java.util.Set;

@Controller
@RequestMapping("/admin/notifications")
public class AdminNotificationController {
    private static final Set<String> TARGET_ROLES = Set.of("ALL", "ROLE_PARENT", "ROLE_TEACHER", "ROLE_ADMIN");
    private final NotificationBroadcastRepository broadcasts;
    private final UserRepository users;

    public AdminNotificationController(NotificationBroadcastRepository broadcasts, UserRepository users) {
        this.broadcasts = broadcasts; this.users = users;
    }

    @GetMapping
    public String index(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        model.addAttribute("currentUser", user);
        model.addAttribute("broadcasts", broadcasts.findTop100ByOrderByCreatedAtDesc());
        return "admin/notifications/index";
    }

    @PostMapping
    public String create(@AuthenticationPrincipal CustomUserDetails user,
                         @RequestParam String title, @RequestParam String body,
                         @RequestParam(defaultValue = "ALL") String targetRole,
                         @RequestParam(defaultValue = "/notifications") String targetPath,
                         @RequestParam(required = false) LocalDateTime scheduledAt,
                         RedirectAttributes redirect) {
        if (title == null || title.isBlank() || title.trim().length() > 180 || body == null || body.isBlank() || body.trim().length() > 500) {
            redirect.addFlashAttribute("errorMessage", "Tiêu đề tối đa 180 ký tự và nội dung tối đa 500 ký tự.");
            return "redirect:/admin/notifications";
        }
        if (!TARGET_ROLES.contains(targetRole)) {
            redirect.addFlashAttribute("errorMessage", "Nhóm người nhận không hợp lệ.");
            return "redirect:/admin/notifications";
        }
        if (targetPath == null || !targetPath.startsWith("/") || targetPath.startsWith("//") || targetPath.length() > 2048
                || targetPath.indexOf('\\') >= 0 || targetPath.chars().anyMatch(Character::isISOControl)) targetPath = "/notifications";
        NotificationBroadcast broadcast = new NotificationBroadcast();
        broadcast.setCreatedBy(users.findById(user.getId()).orElseThrow());
        broadcast.setTargetRole("ALL".equals(targetRole) ? null : targetRole);
        broadcast.setTitle(title.trim()); broadcast.setBody(body.trim()); broadcast.setTargetPath(targetPath);
        broadcast.setScheduledAt(scheduledAt == null ? LocalDateTime.now() : scheduledAt);
        broadcasts.save(broadcast);
        redirect.addFlashAttribute("successMessage", scheduledAt != null && scheduledAt.isAfter(LocalDateTime.now())
                ? "Đã lên lịch gửi thông báo." : "Thông báo sẽ được gửi trong ít phút tới.");
        return "redirect:/admin/notifications";
    }
}
