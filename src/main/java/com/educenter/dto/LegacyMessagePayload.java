package com.educenter.dto;

import com.educenter.entity.ParentTeacherMessage;
import com.educenter.entity.TeacherAdminMessage;

import java.time.LocalDateTime;

public record LegacyMessagePayload(Long id, Long senderId, String senderName, String content, LocalDateTime createdAt) {
    public static LegacyMessagePayload from(ParentTeacherMessage message) {
        return new LegacyMessagePayload(message.getId(), message.getSender().getId(), message.getSender().getFullName(),
                message.getContent(), message.getCreatedAt());
    }

    public static LegacyMessagePayload from(TeacherAdminMessage message) {
        return new LegacyMessagePayload(message.getId(), message.getSender().getId(), message.getSender().getFullName(),
                message.getContent(), message.getCreatedAt());
    }
}
