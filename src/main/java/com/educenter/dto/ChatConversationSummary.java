package com.educenter.dto;

import com.educenter.entity.ChatConversation;
import com.educenter.entity.ChatMessage;

public record ChatConversationSummary(ChatConversation conversation, ChatMessage latestMessage,
                                     long unreadCount, String displayTitle, String initials) {}
