package com.educenter.repository;

import com.educenter.entity.ParentTeacherMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParentTeacherMessageRepository extends JpaRepository<ParentTeacherMessage, Long> {
    List<ParentTeacherMessage> findByConversationIdOrderByCreatedAtAscIdAsc(Long conversationId);
    List<ParentTeacherMessage> findByConversationIdAndIdGreaterThanOrderByIdAsc(Long conversationId, Long afterId);
    List<ParentTeacherMessage> findByConversationIdAndIdGreaterThanAndSenderIdNotOrderByIdDesc(Long conversationId, Long afterId, Long senderId);
    Optional<ParentTeacherMessage> findTopByConversationIdOrderByIdDesc(Long conversationId);
}
