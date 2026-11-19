package com.educenter.repository;

import com.educenter.entity.ParentTeacherMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParentTeacherMessageRepository extends JpaRepository<ParentTeacherMessage, Long> {
    List<ParentTeacherMessage> findByConversationIdOrderByCreatedAtAscIdAsc(Long conversationId);
}
