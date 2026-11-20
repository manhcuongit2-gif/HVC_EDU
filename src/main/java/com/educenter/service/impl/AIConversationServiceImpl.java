package com.educenter.service.impl;

import com.educenter.dto.AIConversationSummary;
import com.educenter.dto.AIMessageView;
import com.educenter.entity.AIConversation;
import com.educenter.entity.AIMessage;
import com.educenter.repository.AIConversationRepository;
import com.educenter.repository.AIMessageRepository;
import com.educenter.repository.UserRepository;
import com.educenter.service.AIConversationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class AIConversationServiceImpl implements AIConversationService {
    private final AIConversationRepository conversationRepository;
    private final AIMessageRepository messageRepository;
    private final UserRepository userRepository;

    public AIConversationServiceImpl(AIConversationRepository conversationRepository,
                                    AIMessageRepository messageRepository, UserRepository userRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AIConversationSummary> list(Long ownerId, String search) {
        String query = search == null || search.isBlank() ? null : search.trim();
        return conversationRepository.searchOwned(ownerId, query).stream().limit(200)
                .map(c -> summary(c, messageRepository.findTop1ByConversationIdOrderByIdDesc(c.getId()).map(AIMessage::getContent).orElse("")))
                .toList();
    }

    @Override
    @Transactional
    public AIConversation create(Long ownerId) {
        return create(ownerId, null, null);
    }

    @Override
    @Transactional
    public AIConversation create(Long ownerId, String contextRole, Long contextId) {
        AIConversation conversation = new AIConversation();
        conversation.setOwner(userRepository.getReferenceById(ownerId));
        conversation.setTitle("Cuộc trò chuyện mới");
        conversation.setContextRole(contextRole);
        conversation.setContextId(contextId);
        return conversationRepository.save(conversation);
    }

    @Override
    @Transactional
    public AIConversation bindContext(Long conversationId, Long ownerId, String contextRole, Long contextId) {
        AIConversation conversation = conversationRepository.findByIdAndOwnerIdForUpdate(conversationId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cuộc trò chuyện."));
        if (conversation.getContextRole() == null && messageRepository.countByConversationId(conversationId) == 0) {
            conversation.setContextRole(contextRole);
            conversation.setContextId(contextId);
            return conversationRepository.save(conversation);
        }
        if (!java.util.Objects.equals(conversation.getContextRole(), contextRole)
                || !java.util.Objects.equals(conversation.getContextId(), contextId))
            throw new IllegalArgumentException("Phạm vi dữ liệu đã đổi. Hãy bắt đầu cuộc trò chuyện mới để tiếp tục.");
        return conversation;
    }

    @Override
    @Transactional(readOnly = true)
    public AIConversation requireOwned(Long conversationId, Long ownerId) {
        return conversationRepository.findByIdAndOwnerId(conversationId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cuộc trò chuyện."));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AIMessageView> messages(Long conversationId, Long ownerId) {
        requireOwned(conversationId, ownerId);
        List<AIMessage> all = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        int start = Math.max(0, all.size() - 200);
        return all.subList(start, all.size()).stream()
                .map(m -> new AIMessageView(m.getId(), m.getSenderRole(), m.getContent(), m.getModel(), m.getCreatedAt())).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AIMessage> recentMessages(Long conversationId, Long ownerId, int limit, int maxCharacters) {
        requireOwned(conversationId, ownerId);
        List<AIMessage> newest = messageRepository.findTop12ByConversationIdOrderByCreatedAtDesc(conversationId);
        return selectContext(newest, limit, maxCharacters);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AIMessage> recentMessagesBefore(Long conversationId, Long ownerId, Long userMessageId, int limit, int maxCharacters) {
        requireOwned(conversationId, ownerId);
        AIMessage target = messageRepository.findByIdAndConversationId(userMessageId, conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy câu hỏi cần chỉnh sửa."));
        if (!"USER".equals(target.getSenderRole())) throw new IllegalArgumentException("Chỉ có thể chỉnh sửa câu hỏi của chính bạn.");
        return selectContext(messageRepository.findTop12ByConversationIdAndIdLessThanOrderByCreatedAtDesc(conversationId, userMessageId), limit, maxCharacters);
    }

    private List<AIMessage> selectContext(List<AIMessage> newest, int limit, int maxCharacters) {
        Collections.reverse(newest);
        List<AIMessage> result = new ArrayList<>();
        int total = 0;
        for (int i = newest.size() - 1; i >= 0 && result.size() < limit; i--) {
            AIMessage message = newest.get(i);
            int length = message.getContent() == null ? 0 : message.getContent().length();
            if (total + length > maxCharacters) break;
            result.add(message);
            total += length;
        }
        Collections.reverse(result);
        return result;
    }

    @Override
    @Transactional
    public AIConversationSummary update(Long conversationId, Long ownerId, String title, Boolean pinned) {
        AIConversation conversation = conversationRepository.findByIdAndOwnerId(conversationId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cuộc trò chuyện."));
        if (title != null) {
            String normalized = title.trim();
            if (normalized.isBlank() || normalized.length() > 160)
                throw new IllegalArgumentException("Tên cuộc trò chuyện phải từ 1 đến 160 ký tự.");
            conversation.setTitle(normalized);
        }
        if (pinned != null) conversation.setPinned(pinned);
        AIConversation saved = conversationRepository.save(conversation);
        String preview = messageRepository.findTop1ByConversationIdOrderByIdDesc(saved.getId()).map(AIMessage::getContent).orElse("");
        return summary(saved, preview);
    }

    @Override
    @Transactional
    public void delete(Long conversationId, Long ownerId) {
        AIConversation conversation = conversationRepository.findByIdAndOwnerId(conversationId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cuộc trò chuyện."));
        conversationRepository.delete(conversation);
    }

    @Override
    @Transactional
    public void truncateFrom(Long conversationId, Long ownerId, Long userMessageId) {
        requireOwned(conversationId, ownerId);
        AIMessage message = messageRepository.findByIdAndConversationId(userMessageId, conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy câu hỏi cần chỉnh sửa."));
        if (!"USER".equals(message.getSenderRole()))
            throw new IllegalArgumentException("Chỉ có thể chỉnh sửa câu hỏi của chính bạn.");
        messageRepository.deleteFromMessage(conversationId, userMessageId);
    }

    @Override
    @Transactional
    public AIMessage saveMessage(AIConversation conversation, String role, String content, String model,
                                 Integer promptTokens, Integer completionTokens) {
        AIMessage message = new AIMessage();
        message.setConversation(conversation);
        message.setSenderRole(role);
        message.setContent(content);
        message.setModel(model);
        message.setPromptTokens(promptTokens);
        message.setCompletionTokens(completionTokens);
        AIMessage saved = messageRepository.save(message);
        conversationRepository.save(conversation);
        return saved;
    }

    @Override
    @Transactional
    public void setFirstTitle(AIConversation conversation, String prompt) {
        if (messageRepository.countByConversationId(conversation.getId()) != 0
                || !"Cuộc trò chuyện mới".equals(conversation.getTitle())) return;
        String title = prompt == null ? "" : prompt.replaceAll("\\s+", " ").trim();
        if (title.length() > 72) title = title.substring(0, 69) + "...";
        if (!title.isBlank()) conversation.setTitle(title);
        conversationRepository.save(conversation);
    }

    private AIConversationSummary summary(AIConversation conversation, String preview) {
        String text = preview == null ? "" : preview.replaceAll("\\s+", " ").trim();
        if (text.length() > 100) text = text.substring(0, 97) + "...";
        return new AIConversationSummary(conversation.getId(), conversation.getTitle(), conversation.isPinned(),
                conversation.getUpdatedAt(), text, conversation.getContextRole(), conversation.getContextId());
    }
}
