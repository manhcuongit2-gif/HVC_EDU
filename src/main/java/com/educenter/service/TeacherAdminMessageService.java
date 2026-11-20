package com.educenter.service;

import com.educenter.dto.TeacherAdminConversationPage;
import com.educenter.dto.LegacyMessagePayload;
import com.educenter.entity.Teacher;

import java.util.List;

public interface TeacherAdminMessageService {
    List<Teacher> findTeachersForAdmin(Long adminUserId);
    TeacherAdminConversationPage openForTeacher(Long teacherUserId);
    TeacherAdminConversationPage openForAdmin(Long adminUserId, Long teacherUserId);
    void sendAsTeacher(Long teacherUserId, String content);
    void sendAsAdmin(Long adminUserId, Long teacherUserId, String content);
    List<LegacyMessagePayload> findMessagesAfterForTeacher(Long teacherUserId, Long afterId);
    List<LegacyMessagePayload> findMessagesAfterForAdmin(Long adminUserId, Long teacherUserId, Long afterId);
}
