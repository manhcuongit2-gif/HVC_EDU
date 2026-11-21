package com.educenter.controller;

import com.educenter.config.PushProperties;
import com.educenter.dto.PushSubscriptionRequest;
import com.educenter.entity.PushSubscription;
import com.educenter.entity.PushSubscriptionUser;
import com.educenter.security.CustomUserDetails;
import com.educenter.service.PushSubscriptionOwnershipException;
import com.educenter.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/push")
public class PushController {
    private final PushProperties pushProperties;
    private final NotificationService notifications;

    public PushController(PushProperties pushProperties, NotificationService notifications) {
        this.pushProperties = pushProperties;
        this.notifications = notifications;
    }

    @GetMapping("/public-key")
    public Map<String, Object> publicKey() {
        return Map.of("enabled", pushProperties.configured(), "publicKey", pushProperties.configured() ? pushProperties.publicKey() : "");
    }

    @PostMapping("/subscriptions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void subscribe(@AuthenticationPrincipal CustomUserDetails user,
                          @RequestBody PushSubscriptionRequest request, HttpServletRequest servletRequest) {
        if (request == null || request.keys() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu khóa mã hóa thiết bị.");
        try {
            notifications.saveSubscription(user.getId(), request.endpoint(), request.keys().p256dh(), request.keys().auth(),
                    request.deviceName(), servletRequest.getHeader("User-Agent"), request.platform());
        } catch (PushSubscriptionOwnershipException | DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Đăng ký thiết bị không khớp dữ liệu đã lưu. Hãy tải lại trang và thử liên kết lại.");
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @GetMapping("/subscriptions")
    public List<Map<String, Object>> subscriptions(@AuthenticationPrincipal CustomUserDetails user) {
        return notifications.subscriptions(user.getId()).stream().map(this::deviceView).toList();
    }

    @DeleteMapping("/subscriptions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsubscribe(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id) {
        try {
            notifications.removeSubscription(user.getId(), id);
        } catch (IllegalArgumentException ex) {
            // Return the same response for an unknown ID and a device owned by someone else.
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thiết bị.");
        }
    }

    private Map<String, Object> deviceView(PushSubscriptionUser link) {
        PushSubscription s = link.getSubscription();
        return Map.of("id", link.getId(), "name", s.getDeviceName(), "platform", s.getPlatform() == null ? "" : s.getPlatform(),
                "lastSeenAt", s.getLastSeenAt() == null ? "" : s.getLastSeenAt().toString(), "endpointHash", s.getEndpointHash());
    }
}
