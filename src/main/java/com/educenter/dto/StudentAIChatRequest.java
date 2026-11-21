package com.educenter.dto;

import jakarta.validation.constraints.Size;

public record StudentAIChatRequest(Long conversationId,
                                  @Size(max = 12000) String message,
                                  String action) {
    public String safeMessage() {
        return message == null ? "" : message.trim();
    }
    public String safeAction() {
        if (action == null || action.isBlank()) return "ASK";
        String value = action.trim().toUpperCase(java.util.Locale.ROOT);
        return java.util.Set.of("ASK", "HINT", "NEXT_HINT", "SOLUTION").contains(value) ? value : "ASK";
    }
}
