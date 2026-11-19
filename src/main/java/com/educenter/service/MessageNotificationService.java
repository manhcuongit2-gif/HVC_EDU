package com.educenter.service;

import com.educenter.dto.MessageNotificationSummary;

public interface MessageNotificationService {
    MessageNotificationSummary getUnreadFor(Long userId, String roleName);
}
