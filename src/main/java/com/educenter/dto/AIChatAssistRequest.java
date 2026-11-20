package com.educenter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AIChatAssistRequest(@NotBlank String action,
                                  @NotNull Long conversationId,
                                  @Size(max = 2000) String draft) {}
