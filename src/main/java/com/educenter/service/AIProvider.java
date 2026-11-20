package com.educenter.service;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public interface AIProvider {
    AIProviderResponse complete(List<Map<String, Object>> messages, String model, int maxTokens,
                               List<Map<String, Object>> tools);
    AIProviderResponse stream(List<Map<String, Object>> messages, String model, int maxTokens,
                              Consumer<String> onDelta);

    record ToolCall(String id, String name, String arguments) {}
    record AIProviderResponse(String content, List<ToolCall> toolCalls, long promptTokens,
                              long completionTokens, Integer providerStatus,
                              Long remainingRequests, Long remainingTokens) {}
}
