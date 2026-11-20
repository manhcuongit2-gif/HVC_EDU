package com.educenter.dto;

public record AISettingsUpdate(boolean enabled,
                               boolean adminEnabled,
                               boolean teacherEnabled,
                               boolean parentEnabled,
                               Integer perUserDailyRequests,
                               Integer globalDailyRequests,
                               Long dailyTokenLimit,
                               Integer maxPromptCharacters,
                               Integer maxResponseTokens,
                               Integer maxConcurrentRequests,
                               String selectedModel) {}
