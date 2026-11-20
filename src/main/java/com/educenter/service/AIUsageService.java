package com.educenter.service;

import com.educenter.dto.AISettingsUpdate;
import com.educenter.dto.AIUsageSummary;
import com.educenter.entity.AIConversation;
import com.educenter.entity.AISettings;
import com.educenter.entity.AIUsage;
import java.util.Map;

public interface AIUsageService {
    AIUsage reserve(Long userId, String role, AIConversation conversation, String model, int promptChars);
    void complete(Long usageId, long promptTokens, long completionTokens, Integer providerStatus,
                  Long remainingRequests, Long remainingTokens);
    void fail(Long usageId, AIException error);
    AIUsageSummary summary(Long userId);
    Map<String, Object> adminSummary();
    AISettings settings();
    AISettings updateSettings(AISettingsUpdate update);
}
