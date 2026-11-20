package com.educenter.dto;

import java.time.LocalDateTime;

public record InboxConversationView(
        String category,
        String categoryLabel,
        String title,
        String subtitle,
        String preview,
        String initials,
        String url,
        LocalDateTime updatedAt,
        long unreadCount,
        boolean group) {
}
