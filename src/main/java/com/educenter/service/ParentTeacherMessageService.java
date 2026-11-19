package com.educenter.service;

import com.educenter.dto.ConversationPage;
import com.educenter.dto.ConversationTarget;
import com.educenter.entity.ClassEntity;

import java.util.List;

public interface ParentTeacherMessageService {
    List<ClassEntity> findParentClasses(Long parentUserId);
    List<ConversationTarget> findTeacherTargets(Long teacherUserId);
    ConversationPage openForParent(Long parentUserId, Long classId);
    ConversationPage openForTeacher(Long teacherUserId, Long classId, Long parentId);
    void sendAsParent(Long parentUserId, Long classId, String content);
    void sendAsTeacher(Long teacherUserId, Long classId, Long parentId, String content);
}
