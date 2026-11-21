package com.educenter.service.impl;

import com.educenter.entity.Role;
import com.educenter.entity.Student;
import com.educenter.entity.User;
import com.educenter.repository.*;
import com.educenter.service.NotificationService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class HomeworkServiceStudentScopeTest {
    private final ClassRepository classes = mock(ClassRepository.class);
    private final ClassStudentRepository enrollments = mock(ClassStudentRepository.class);
    private final HomeworkAssignmentRepository assignments = mock(HomeworkAssignmentRepository.class);
    private final HomeworkSubmissionRepository submissions = mock(HomeworkSubmissionRepository.class);
    private final StudentRepository students = mock(StudentRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final NotificationService notifications = mock(NotificationService.class);
    private final HomeworkServiceImpl service = new HomeworkServiceImpl(classes, enrollments, assignments, submissions,
            students, users, notifications, "target/test-uploads");

    @Test
    void studentCannotReadAnotherStudentsSubmissionFile() {
        User account = new User();
        account.setId(7L);
        account.setRole(new Role(4, "ROLE_STUDENT", "Student"));
        account.setActive(true);
        Student linkedStudent = new Student();
        linkedStudent.setId(11L);
        linkedStudent.setUser(account);
        linkedStudent.setActive(true);
        Student otherStudent = new Student();
        otherStudent.setId(22L);
        var submission = new com.educenter.entity.HomeworkSubmission();
        submission.setStudent(otherStudent);
        when(students.findByUserId(7L)).thenReturn(Optional.of(linkedStudent));
        when(submissions.findById(99L)).thenReturn(Optional.of(submission));

        assertThrows(IllegalArgumentException.class, () -> service.submissionFileForStudent(7L, 99L));
    }

    @Test
    void accountWithoutStudentProfileCannotListAssignments() {
        when(students.findByUserId(7L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.findForStudent(7L));

        verifyNoInteractions(enrollments, assignments, submissions);
    }
}
