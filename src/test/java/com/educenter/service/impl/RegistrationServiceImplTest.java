package com.educenter.service.impl;

import com.educenter.entity.ClassEntity;
import com.educenter.entity.Parent;
import com.educenter.entity.Registration;
import com.educenter.entity.Student;
import com.educenter.entity.User;
import com.educenter.repository.ClassStudentRepository;
import com.educenter.repository.RegistrationRepository;
import com.educenter.repository.TuitionRepository;
import com.educenter.repository.UserRepository;
import com.educenter.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RegistrationServiceImplTest {
    private final RegistrationRepository registrations = mock(RegistrationRepository.class);
    private final ClassStudentRepository enrollments = mock(ClassStudentRepository.class);
    private final TuitionRepository tuitions = mock(TuitionRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final NotificationService notifications = mock(NotificationService.class);
    private RegistrationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RegistrationServiceImpl(registrations, mock(com.educenter.repository.ParentRepository.class),
                mock(com.educenter.repository.StudentRepository.class), mock(com.educenter.repository.ClassRepository.class), enrollments,
                tuitions, users, notifications);
    }

    @Test
    void approvingEnrollmentDoesNotCreateCourseBasedTuition() {
        User parentUser = new User();
        parentUser.setId(40L);
        Parent parent = new Parent();
        parent.setUser(parentUser);
        Student student = new Student();
        student.setId(30L);
        student.setParent(parent);
        ClassEntity classEntity = new ClassEntity();
        classEntity.setId(20L);
        classEntity.setName("Math 7");
        classEntity.setMaxStudents(25);
        Registration registration = new Registration();
        registration.setId(10L);
        registration.setParent(parent);
        registration.setStudent(student);
        registration.setClassEntity(classEntity);

        when(registrations.findById(10L)).thenReturn(Optional.of(registration));
        when(enrollments.countByClassEntityIdAndStatus(20L, "ACTIVE")).thenReturn(0L);
        when(enrollments.findByClassEntityIdAndStudentId(20L, 30L)).thenReturn(Optional.empty());
        when(users.findById(99L)).thenReturn(Optional.empty());

        service.approveRegistration(10L, 99L, "Approved");

        verify(enrollments).save(any());
        verify(tuitions, never()).save(any());
        verify(notifications).notifyUser(eq(40L), eq("registration-processed-10"), eq("GENERAL"),
                any(), any(), eq("/parent/registrations"));
    }
}
