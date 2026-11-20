package com.educenter.repository;

import com.educenter.entity.ChatMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatMemberRepository extends JpaRepository<ChatMember, Long> {
    @Query("select m from ChatMember m join fetch m.conversation c where m.user.id = :userId and m.archived = false order by c.updatedAt desc")
    List<ChatMember> findActiveConversationsForUser(@Param("userId") Long userId);
    List<ChatMember> findByConversationIdOrderByJoinedAtAsc(Long conversationId);
    Optional<ChatMember> findByConversationIdAndUserId(Long conversationId, Long userId);
    boolean existsByConversationIdAndUserId(Long conversationId, Long userId);
    long countByConversationId(Long conversationId);
    void deleteByConversationIdAndUserId(Long conversationId, Long userId);
}
