package com.educenter.service.impl;

import com.educenter.entity.ClassEntity;
import com.educenter.entity.Teacher;
import com.educenter.entity.User;
import com.educenter.repository.ClassRepository;
import com.educenter.repository.ClassStudentRepository;
import com.educenter.repository.ParentRepository;
import com.educenter.repository.StudentRepository;
import com.educenter.service.ClassService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TeacherStudentServiceImplTest {

    @Test
    void teacherCannotReadRosterOfAnotherTeachersClass() {
        ClassRepository classRepository = mock(ClassRepository.class);
        ClassStudentRepository classStudentRepository = mock(ClassStudentRepository.class);
        StudentRepository studentRepository = mock(StudentRepository.class);
        ParentRepository parentRepository = mock(ParentRepository.class);
        ClassService classService = mock(ClassService.class);
        TeacherStudentServiceImpl service = new TeacherStudentServiceImpl(
                classRepository, classStudentRepository, studentRepository, parentRepository, classService);

        User owner = new User();
        owner.setId(22L);
        Teacher teacher = new Teacher();
        teacher.setUser(owner);
        ClassEntity classEntity = new ClassEntity();
        classEntity.setTeacher(teacher);
        when(classRepository.findById(5L)).thenReturn(Optional.of(classEntity));

        assertThrows(IllegalArgumentException.class, () -> service.getRoster(11L, 5L));
        verify(classStudentRepository, never()).findByClassEntityIdAndStatus(5L, "ACTIVE");
    }
}
