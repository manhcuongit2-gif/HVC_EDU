package com.educenter.dto;

import java.util.List;

public record MessageNotificationSummary(long unreadCount, List<MessageNotification> items) {}
