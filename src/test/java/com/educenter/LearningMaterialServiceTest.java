package com.educenter;

import com.educenter.entity.ClassEntity;
import com.educenter.entity.LearningMaterial;
import com.educenter.entity.Teacher;
import com.educenter.entity.User;
import com.educenter.repository.ClassRepository;
import com.educenter.repository.ClassStudentRepository;
import com.educenter.repository.LearningMaterialRepository;
import com.educenter.repository.UserRepository;
import com.educenter.service.impl.LearningMaterialServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LearningMaterialServiceTest {

    @TempDir
    Path uploadDirectory;

    private LearningMaterialRepository materialRepository;
    private ClassRepository classRepository;
    private ClassStudentRepository classStudentRepository;
    private UserRepository userRepository;
    private LearningMaterialServiceImpl service;
    private ClassEntity teacherClass;
    private User teacherUser;

    @BeforeEach
    void setUp() {
        materialRepository = mock(LearningMaterialRepository.class);
        classRepository = mock(ClassRepository.class);
        classStudentRepository = mock(ClassStudentRepository.class);
        userRepository = mock(UserRepository.class);
        service = new LearningMaterialServiceImpl(materialRepository, classRepository, classStudentRepository,
                userRepository, uploadDirectory.toString());

        teacherUser = new User();
        teacherUser.setId(11L);
        teacherUser.setFullName("Giáo viên");
        Teacher teacher = new Teacher();
        teacher.setUser(teacherUser);
        teacherClass = new ClassEntity();
        teacherClass.setId(21L);
        teacherClass.setTeacher(teacher);
        when(classRepository.findById(21L)).thenReturn(Optional.of(teacherClass));
        when(userRepository.findById(11L)).thenReturn(Optional.of(teacherUser));
        when(materialRepository.save(any(LearningMaterial.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void teacherCanUploadSupportedFileForOwnClass() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "lesson.pdf", "application/pdf", "lesson".getBytes());

        LearningMaterial saved = service.createForTeacher(21L, "Bài học", "Tài liệu buổi học", null, file, 11L);

        assertEquals("Bài học", saved.getTitle());
        assertEquals("lesson.pdf", saved.getOriginalFilename());
        assertTrue(Files.isRegularFile(service.resolveStoredFile(saved)));
        assertTrue(service.resolveStoredFile(saved).startsWith(uploadDirectory));
    }

    @Test
    void teacherCannotPostForAnotherTeachersClass() {
        User otherUser = new User();
        otherUser.setId(12L);
        Teacher otherTeacher = new Teacher();
        otherTeacher.setUser(otherUser);
        ClassEntity otherClass = new ClassEntity();
        otherClass.setId(22L);
        otherClass.setTeacher(otherTeacher);
        when(classRepository.findById(22L)).thenReturn(Optional.of(otherClass));

        assertThrows(IllegalArgumentException.class,
                () -> service.createForTeacher(22L, "Bài học", null, "https://example.com/lesson", null, 11L));
        verify(materialRepository, never()).save(any());
    }

    @Test
    void rejectsUnsupportedFileTypeAndFilesLargerThanTwentyMegabytes() {
        MockMultipartFile executable = new MockMultipartFile("file", "lesson.exe", "application/octet-stream", new byte[]{1});
        assertThrows(IllegalArgumentException.class,
                () -> service.createForTeacher(21L, "Tài liệu", null, null, executable, 11L));

        byte[] oversized = new byte[20 * 1024 * 1024 + 1];
        MockMultipartFile largePdf = new MockMultipartFile("file", "lesson.pdf", "application/pdf", oversized);
        assertThrows(IllegalArgumentException.class,
                () -> service.createForTeacher(21L, "Tài liệu", null, null, largePdf, 11L));
    }

    @Test
    void parentCannotDownloadMaterialOutsideActiveEnrolledClasses() {
        LearningMaterial material = new LearningMaterial();
        material.setId(31L);
        material.setClassEntity(teacherClass);
        material.setStoredFilename("document.pdf");
        when(materialRepository.findById(31L)).thenReturn(Optional.of(material));
        when(classStudentRepository.findByStudentParentUserId(99L)).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () -> service.findFileForParent(31L, 99L));
    }
}
