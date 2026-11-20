package com.educenter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AIChatRequest(Long conversationId,
                            Long contextId,
                            Long editMessageId,
                            @NotBlank @Size(max = 50000) String message) {}
