package com.educenter.repository;

import com.educenter.entity.TeacherAdminConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeacherAdminConversationRepository extends JpaRepository<TeacherAdminConversation, Long> {
    Optional<TeacherAdminConversation> findByTeacherUserIdAndAdminUserId(Long teacherUserId, Long adminUserId);
    List<TeacherAdminConversation> findByAdminUserIdOrderByUpdatedAtDesc(Long adminUserId);
    List<TeacherAdminConversation> findByTeacherUserIdOrderByUpdatedAtDesc(Long teacherUserId);
}
