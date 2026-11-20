package com.educenter.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AIReportRequest(@NotNull Long sessionId,
                              Long studentId,
                              @Size(max = 1000) String additionalContext) {}
