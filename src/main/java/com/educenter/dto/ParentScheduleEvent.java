package com.educenter.dto;

import com.educenter.entity.ClassSession;

import java.util.List;

public record ParentScheduleEvent(ClassSession session, List<String> childNames, List<ParentChildAttendance> attendance,
                                  boolean reportAvailable,
                                  String color) {}
