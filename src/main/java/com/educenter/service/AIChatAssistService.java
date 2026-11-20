package com.educenter.service;

public interface AIChatAssistService {
    String createDraft(Long userId, String role, Long conversationId, String action, String draft);
}
