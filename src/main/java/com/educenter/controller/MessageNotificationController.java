package com.educenter.controller;

import com.educenter.dto.MessageNotificationSummary;
import com.educenter.security.CustomUserDetails;
import com.educenter.service.MessageNotificationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class MessageNotificationController {

    private final MessageNotificationService notificationService;

    public MessageNotificationController(MessageNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public MessageNotificationSummary unread(@AuthenticationPrincipal CustomUserDetails user) {
        return notificationService.getUnreadFor(user.getId(), user.getRoleName());
    }
}
