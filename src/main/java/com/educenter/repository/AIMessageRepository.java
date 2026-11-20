package com.educenter.repository;

import com.educenter.entity.AIMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface AIMessageRepository extends JpaRepository<AIMessage, Long> {
    List<AIMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
    List<AIMessage> findTop12ByConversationIdOrderByCreatedAtDesc(Long conversationId);
    List<AIMessage> findTop12ByConversationIdAndIdLessThanOrderByCreatedAtDesc(Long conversationId, Long id);
    java.util.Optional<AIMessage> findTop1ByConversationIdOrderByIdDesc(Long conversationId);
    long countByConversationId(Long conversationId);
    java.util.Optional<AIMessage> findByIdAndConversationId(Long id, Long conversationId);
    @Modifying
    @Query("delete from AIMessage m where m.conversation.id = :conversationId and m.id >= :messageId")
    void deleteFromMessage(@Param("conversationId") Long conversationId, @Param("messageId") Long messageId);
}
