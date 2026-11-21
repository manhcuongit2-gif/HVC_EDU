package com.educenter.service.impl;

import com.educenter.entity.Role;
import com.educenter.entity.Student;
import com.educenter.entity.User;
import com.educenter.repository.ParentRepository;
import com.educenter.repository.RoleRepository;
import com.educenter.repository.StudentRepository;
import com.educenter.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class StudentServiceAccountTest {
    private final StudentRepository students = mock(StudentRepository.class);
    private final ParentRepository parents = mock(ParentRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final RoleRepository roles = mock(RoleRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final StudentServiceImpl service = new StudentServiceImpl(students, parents, users, roles, encoder);

    @Test
    void adminCannotLinkAParentOrTeacherAccountToStudentProfile() {
        Student student = new Student();
        student.setId(12L);
        User account = new User();
        account.setId(34L);
        account.setRole(new Role(2, "ROLE_TEACHER", "Teacher"));
        account.setActive(true);
        when(students.findById(12L)).thenReturn(Optional.of(student));
        when(users.findById(34L)).thenReturn(Optional.of(account));

        assertThrows(IllegalArgumentException.class, () -> service.linkStudentAccount(12L, 34L));
        verify(students, never()).save(any());
    }

    @Test
    void accountCannotBeLinkedToTwoStudentProfiles() {
        Student student = new Student();
        student.setId(12L);
        User account = new User();
        account.setId(34L);
        account.setRole(new Role(4, "ROLE_STUDENT", "Student"));
        account.setActive(true);
        when(students.findById(12L)).thenReturn(Optional.of(student));
        when(users.findById(34L)).thenReturn(Optional.of(account));
        when(students.existsByUserId(34L)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.linkStudentAccount(12L, 34L));
        verify(students, never()).save(any());
    }
}
