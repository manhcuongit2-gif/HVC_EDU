package com.educenter.service;

import com.educenter.dto.ChatConversationSummary;
import com.educenter.dto.ChatMessagePayload;
import com.educenter.dto.ChatPage;
import com.educenter.entity.ChatConversation;
import com.educenter.entity.ChatMessage;
import com.educenter.entity.ChatMessageReaction;
import com.educenter.entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

public interface ChatService {
    List<User> findContacts(Long userId, String roleName);
    List<ChatConversationSummary> findConversations(Long userId, String search);
    ChatConversation createConversation(Long creatorId, String creatorRole, List<Long> recipientIds, String title);
    ChatPage openConversation(Long conversationId, Long userId);
    ChatMessage send(Long conversationId, Long senderId, String content, Long replyToId, MultipartFile file,
                     List<Long> mentionedUserIds, boolean important);
    List<ChatMessagePayload> findMessagesAfter(Long conversationId, Long userId, Long afterId);
    ChatMessage findMessageForMember(Long conversationId, Long messageId, Long userId);
    Path resolveFile(ChatMessage message);
    void setAvatar(Long conversationId, Long userId, MultipartFile file);
    Path resolveAvatar(Long conversationId, Long userId);
    ChatMessageReaction toggleReaction(Long conversationId, Long messageId, Long userId, String emoji);
    void deleteForAll(Long conversationId, Long messageId, Long userId);
    ChatMessage forward(Long sourceConversationId, Long messageId, Long targetConversationId, Long userId);
    void rename(Long conversationId, Long userId, String title);
    void addMembers(Long conversationId, Long actorId, List<Long> userIds);
    void removeMember(Long conversationId, Long actorId, Long memberId);
    void archive(Long conversationId, Long userId);
    void setNotifications(Long conversationId, Long userId, boolean enabled);
    ChatConversation sendAdminMessageToParent(Long adminUserId, Long parentUserId, String content);
}
