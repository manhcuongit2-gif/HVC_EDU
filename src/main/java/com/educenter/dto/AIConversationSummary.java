package com.educenter.dto;

import java.time.LocalDateTime;

public record AIConversationSummary(Long id, String title, boolean pinned,
                                    LocalDateTime updatedAt, String preview,
                                    String contextRole, Long contextId) {}
