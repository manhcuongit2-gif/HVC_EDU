package com.educenter.repository;

import com.educenter.entity.TeacherAdminMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeacherAdminMessageRepository extends JpaRepository<TeacherAdminMessage, Long> {
    List<TeacherAdminMessage> findByConversationIdOrderByCreatedAtAscIdAsc(Long conversationId);
}
