package com.educenter.dto;

import com.educenter.entity.TeacherAdminConversation;
import com.educenter.entity.TeacherAdminMessage;

import java.util.List;

public record TeacherAdminConversationPage(TeacherAdminConversation conversation, List<TeacherAdminMessage> messages) {}
