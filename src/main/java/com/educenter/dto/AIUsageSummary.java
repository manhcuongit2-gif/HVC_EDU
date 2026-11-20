package com.educenter.dto;

import java.util.Map;

public record AIUsageSummary(long requestsToday, long tokensToday, long failedToday,
                             long remainingRequests, long remainingTokens,
                             Map<String, Long> requestsByRole) {}
