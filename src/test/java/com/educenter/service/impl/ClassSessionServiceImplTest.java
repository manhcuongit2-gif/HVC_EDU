package com.educenter.service.impl;

import com.educenter.entity.ClassEntity;
import com.educenter.entity.ClassSession;
import com.educenter.entity.Room;
import com.educenter.entity.Schedule;
import com.educenter.repository.ClassRepository;
import com.educenter.repository.ClassSessionRepository;
import com.educenter.repository.RoomRepository;
import com.educenter.repository.ScheduleRepository;
import com.educenter.repository.SessionStudentReportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClassSessionServiceImplTest {

    @Mock private ClassSessionRepository sessionRepository;
    @Mock private ClassRepository classRepository;
    @Mock private ScheduleRepository scheduleRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private SessionStudentReportRepository reportRepository;

    @Test
    void generatesDatedOccurrencesForScheduleWeekdayWithinClassRange() {
        ClassEntity course = new ClassEntity();
        course.setId(2L);
        course.setStartDate(LocalDate.of(2026, 11, 2)); // Monday
        course.setEndDate(LocalDate.of(2026, 11, 8));
        Schedule schedule = new Schedule();
        schedule.setId(4L);
        schedule.setClassEntity(course);
        schedule.setDayOfWeek(2);
        schedule.setStartTime(LocalTime.of(17, 30));
        schedule.setEndTime(LocalTime.of(19, 30));
        schedule.setRoom(new Room());
        schedule.setActive(true);
        when(classRepository.findById(2L)).thenReturn(Optional.of(course));
        when(sessionRepository.findByClassEntityId(2L)).thenReturn(List.of());
        when(reportRepository.findReportedSessionIdsForClass(2L)).thenReturn(List.of());
        when(scheduleRepository.findByClassEntityId(2L)).thenReturn(List.of(schedule));

        new ClassSessionServiceImpl(sessionRepository, classRepository, scheduleRepository, roomRepository, reportRepository)
                .synchronizeClass(2L);

        ArgumentCaptor<ClassSession> captor = ArgumentCaptor.forClass(ClassSession.class);
        verify(sessionRepository).save(captor.capture());
        ClassSession session = captor.getValue();
        assertEquals(LocalDate.of(2026, 11, 2), session.getOccurrenceDate());
        assertEquals(LocalDate.of(2026, 11, 2), session.getSessionDate());
        assertEquals(LocalTime.of(17, 30), session.getStartTime());
        assertEquals("SCHEDULED", session.getStatus());
    }

    @Test
    void doesNotGenerateSessionsWhenCourseDatesAreMissing() {
        ClassEntity course = new ClassEntity();
        course.setId(2L);
        when(classRepository.findById(2L)).thenReturn(Optional.of(course));
        when(sessionRepository.findByClassEntityId(2L)).thenReturn(List.of());
        when(reportRepository.findReportedSessionIdsForClass(2L)).thenReturn(List.of());

        new ClassSessionServiceImpl(sessionRepository, classRepository, scheduleRepository, roomRepository, reportRepository)
                .synchronizeClass(2L);

        verifyNoInteractions(scheduleRepository);
        verify(sessionRepository, never()).save(any(ClassSession.class));
    }
}
