package com.educenter.service;

import com.educenter.dto.AIConversationSummary;
import com.educenter.dto.AIMessageView;
import com.educenter.entity.AIConversation;
import com.educenter.entity.AIMessage;
import java.util.List;

public interface AIConversationService {
    List<AIConversationSummary> list(Long ownerId, String search);
    AIConversation create(Long ownerId);
    AIConversation create(Long ownerId, String contextRole, Long contextId);
    AIConversation bindContext(Long conversationId, Long ownerId, String contextRole, Long contextId);
    AIConversation requireOwned(Long conversationId, Long ownerId);
    List<AIMessageView> messages(Long conversationId, Long ownerId);
    List<AIMessage> recentMessages(Long conversationId, Long ownerId, int limit, int maxCharacters);
    List<AIMessage> recentMessagesBefore(Long conversationId, Long ownerId, Long userMessageId, int limit, int maxCharacters);
    AIConversationSummary update(Long conversationId, Long ownerId, String title, Boolean pinned);
    void delete(Long conversationId, Long ownerId);
    void truncateFrom(Long conversationId, Long ownerId, Long userMessageId);
    AIMessage saveMessage(AIConversation conversation, String role, String content, String model,
                          Integer promptTokens, Integer completionTokens);
    void setFirstTitle(AIConversation conversation, String prompt);
}
