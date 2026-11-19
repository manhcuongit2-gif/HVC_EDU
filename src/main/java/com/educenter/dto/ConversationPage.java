package com.educenter.dto;

import com.educenter.entity.ParentTeacherConversation;
import com.educenter.entity.ParentTeacherMessage;

import java.util.List;

public record ConversationPage(ParentTeacherConversation conversation, List<ParentTeacherMessage> messages) {}
