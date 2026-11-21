package com.educenter.service.impl;

import com.educenter.entity.Student;
import com.educenter.repository.*;
import com.educenter.service.ClassSessionService;
import com.educenter.service.HomeworkService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class StudentPortalServiceImplTest {
    private final StudentRepository students = mock(StudentRepository.class);
    private final ClassStudentRepository enrollments = mock(ClassStudentRepository.class);
    private final ClassSessionRepository sessions = mock(ClassSessionRepository.class);
    private final ClassSessionService classSessionService = mock(ClassSessionService.class);
    private final HomeworkService homework = mock(HomeworkService.class);
    private final GradeRepository grades = mock(GradeRepository.class);
    private final AttendanceRepository attendance = mock(AttendanceRepository.class);
    private final SessionStudentReportRepository sessionReports = mock(SessionStudentReportRepository.class);
    private final MonthlyStudentReportRepository monthlyReports = mock(MonthlyStudentReportRepository.class);
    private final StudentPortalServiceImpl service = new StudentPortalServiceImpl(students, enrollments, sessions,
            classSessionService, homework, grades, attendance, sessionReports, monthlyReports);

    @Test
    void rejectsAccountWithoutLinkedStudentProfileBeforeReadingStudentData() {
        when(students.findByUserId(77L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.getPortalData(77L));

        verifyNoInteractions(enrollments, sessions, homework, grades, attendance, sessionReports, monthlyReports);
    }

    @Test
    void rejectsInactiveStudentProfileBeforeReadingStudentData() {
        Student student = new Student();
        student.setId(12L);
        student.setActive(false);
        when(students.findByUserId(77L)).thenReturn(Optional.of(student));

        assertThrows(IllegalArgumentException.class, () -> service.getPortalData(77L));

        verifyNoInteractions(enrollments, sessions, homework, grades, attendance, sessionReports, monthlyReports);
    }
}
