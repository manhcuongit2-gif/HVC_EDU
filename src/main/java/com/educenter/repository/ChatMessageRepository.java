package com.educenter.repository;

import com.educenter.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByConversationIdOrderByIdAsc(Long conversationId);
    Optional<ChatMessage> findTopByConversationIdOrderByIdDesc(Long conversationId);
    List<ChatMessage> findByConversationIdAndIdGreaterThanOrderByIdAsc(Long conversationId, Long afterId);
    long countByConversationIdAndIdGreaterThanAndSenderIdNot(Long conversationId, Long afterId, Long senderId);
    Optional<ChatMessage> findByIdAndConversationId(Long id, Long conversationId);
    boolean existsByConversationIdAndContentContainingIgnoreCase(Long conversationId, String content);
}
