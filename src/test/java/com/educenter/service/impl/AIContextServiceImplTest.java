package com.educenter.service.impl;

import com.educenter.entity.Student;
import com.educenter.entity.User;
import com.educenter.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class AIContextServiceImplTest {
    @Test
    void formatsParentTuitionDataLocallyWithoutModelDisclosure() {
        AIContextServiceImpl service = new AIContextServiceImpl(mock(UserRepository.class), mock(ParentRepository.class),
                mock(StudentRepository.class), mock(TeacherRepository.class), mock(ClassRepository.class),
                mock(ClassStudentRepository.class), mock(ClassSessionRepository.class), mock(GradeRepository.class),
                mock(AttendanceRepository.class), mock(TuitionRepository.class), mock(PaymentRepository.class),
                mock(RegistrationRepository.class), mock(SessionStudentReportRepository.class),
                mock(HomeworkAssignmentRepository.class), mock(HomeworkSubmissionRepository.class), new ObjectMapper());

        String answer = service.localAnswer("ROLE_PARENT", "TUITION",
                "{\"month\":\"2026-11\",\"paid\":1000000,\"unpaidOrProcessing\":250000,\"items\":[{\"class\":\"Toán 7\",\"amount\":250000,\"status\":\"UNPAID\",\"dueDate\":\"2026-11-30\"}]}");

        assertTrue(answer.contains("1.000.000"));
        assertTrue(answer.contains("250.000"));
        assertTrue(answer.contains("Chưa thanh toán"));
        assertTrue(service.localAnswer("ROLE_TEACHER", "TUITION", "{}") == null);
    }

    @Test
    void sanitizesNamesAndContactDetailsBeforeContextLeavesTheServer() {
        User admin = new User();
        admin.setFullName("Admin Example");
        Student student = new Student();
        student.setFullName("Nguyen Van An");
        UserRepository users = mock(UserRepository.class);
        StudentRepository students = mock(StudentRepository.class);
        when(users.findById(1L)).thenReturn(Optional.of(admin));
        when(students.findAll()).thenReturn(List.of(student));

        AIContextServiceImpl service = new AIContextServiceImpl(users, mock(ParentRepository.class), students,
                mock(TeacherRepository.class), mock(ClassRepository.class), mock(ClassStudentRepository.class),
                mock(ClassSessionRepository.class), mock(GradeRepository.class), mock(AttendanceRepository.class),
                mock(TuitionRepository.class), mock(PaymentRepository.class), mock(RegistrationRepository.class),
                mock(SessionStudentReportRepository.class), mock(HomeworkAssignmentRepository.class),
                mock(HomeworkSubmissionRepository.class), new ObjectMapper());

        String sanitized = service.sanitizeText(1L, "ROLE_ADMIN", null,
                "Admin Example asked about Nguyen Van An at family@example.test, phone 0912345678, địa chỉ: 12 Main Street");

        assertFalse(sanitized.contains("Admin Example"));
        assertFalse(sanitized.contains("Nguyen Van An"));
        assertFalse(sanitized.contains("family@example.test"));
        assertFalse(sanitized.contains("0912345678"));
        assertFalse(sanitized.contains("12 Main Street"));
        assertTrue(sanitized.contains("[email đã ẩn]"));
        assertTrue(sanitized.contains("[số điện thoại đã ẩn]"));
        assertTrue(sanitized.contains("[địa chỉ đã ẩn]"));
    }
}
