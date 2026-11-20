package com.educenter.repository;

import com.educenter.entity.TeacherAdminMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeacherAdminMessageRepository extends JpaRepository<TeacherAdminMessage, Long> {
    List<TeacherAdminMessage> findByConversationIdOrderByCreatedAtAscIdAsc(Long conversationId);
    List<TeacherAdminMessage> findByConversationIdAndIdGreaterThanOrderByIdAsc(Long conversationId, Long afterId);
    List<TeacherAdminMessage> findByConversationIdAndIdGreaterThanAndSenderIdNotOrderByIdDesc(Long conversationId, Long afterId, Long senderId);
    Optional<TeacherAdminMessage> findTopByConversationIdOrderByIdDesc(Long conversationId);
}
