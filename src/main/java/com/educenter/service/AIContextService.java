package com.educenter.service;

import java.util.List;
import java.util.Map;

public interface AIContextService {
    List<Map<String, Object>> toolDefinitions(String role);
    AIContextResult execute(Long userId, String role, Long contextId, String topic, String period);
    String localAnswer(String role, String topic, String jsonData);
    String sanitizeText(Long userId, String role, Long contextId, String text);
    List<String> sanitizeTexts(Long userId, String role, Long contextId, List<String> texts);

    record AIContextResult(String content, Map<String, String> replacements) {}
}
