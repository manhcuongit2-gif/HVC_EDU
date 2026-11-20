package com.educenter.dto;

import java.time.LocalDateTime;

public record AIMessageView(Long id, String senderRole, String content,
                            String model, LocalDateTime createdAt) {}
