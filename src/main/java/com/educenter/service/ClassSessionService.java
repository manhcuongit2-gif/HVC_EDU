package com.educenter.service;

import com.educenter.entity.ClassSession;
import com.educenter.entity.Room;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ClassSessionService {
    void synchronizeAllClasses();
    void synchronizeClass(Long classId);
    List<ClassSession> findBetween(LocalDate from, LocalDate to, Long classId);
    List<ClassSession> findForTeacher(Long userId, LocalDate from, LocalDate to);
    ClassSession getById(Long id);
    void cancel(Long sessionId);
    void reschedule(Long sessionId, LocalDate date, LocalTime startTime, LocalTime endTime, Long roomId);
    void beforeScheduleDelete(Long scheduleId);
}
