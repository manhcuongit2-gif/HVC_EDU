package com.educenter.service;

import com.educenter.dto.AIChatRequest;
import java.util.function.Consumer;

public interface AIService {
    void chat(Long userId, String role, AIChatRequest request, Consumer<AIStreamEvent> events);
    String generateText(Long userId, String role, String systemPrompt, String userPrompt);
    record AIStreamEvent(String type, Object data) {}
}
