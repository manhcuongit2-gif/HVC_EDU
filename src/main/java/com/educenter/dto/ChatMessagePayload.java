package com.educenter.dto;

import java.time.LocalDateTime;

public record ChatMessagePayload(Long id, Long senderId, String senderName, String content,
                                 String fileName, String fileUrl, String contentType,
                                 String replyText, boolean deleted, LocalDateTime createdAt) {}
