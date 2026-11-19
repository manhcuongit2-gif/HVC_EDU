package com.educenter.dto;

import com.educenter.entity.ClassEntity;
import com.educenter.entity.Parent;

import java.util.List;

public record ConversationTarget(ClassEntity classEntity, Parent parent, List<String> studentNames) {}
