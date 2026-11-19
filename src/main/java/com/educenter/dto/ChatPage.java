package com.educenter.dto;

import com.educenter.entity.ChatConversation;
import com.educenter.entity.ChatMember;
import com.educenter.entity.ChatMessage;

import java.util.List;

public record ChatPage(ChatConversation conversation, ChatMember currentMember,
                       List<ChatMember> members, List<ChatMessage> messages, String displayTitle) {}
