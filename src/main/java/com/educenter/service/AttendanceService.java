package com.educenter.service;

import com.educenter.entity.Attendance;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceService {
    List<Attendance> getAttendanceByClassAndDate(Long classId, LocalDate date);
    List<Attendance> getAttendanceByStudent(Long studentId);
    void saveAttendanceBatch(Long classId, LocalDate date, List<Long> studentIds, List<String> statuses, List<String> notes, Long teacherUserId);
}
