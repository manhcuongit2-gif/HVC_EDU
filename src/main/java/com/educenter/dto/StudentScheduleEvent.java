package com.educenter.dto;

import com.educenter.entity.ClassSession;

public record StudentScheduleEvent(ClassSession session, String attendanceStatus, String attendanceNote,
                                   String color) {
}
