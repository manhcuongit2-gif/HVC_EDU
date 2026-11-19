package com.educenter.dto;

import com.educenter.entity.ClassSession;

import java.util.List;

public record ParentScheduleEvent(ClassSession session, List<String> childNames, boolean reportAvailable,
                                  String color) {}
