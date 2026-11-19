package com.educenter.dto;

import java.time.LocalDateTime;

public record MessageNotification(
        String senderName,
        String preview,
        LocalDateTime createdAt,
        String conversationUrl
) {}
