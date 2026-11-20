package com.educenter.dto;

import jakarta.validation.constraints.Size;

public record AIConversationUpdate(@Size(max = 160) String title, Boolean pinned) {}
