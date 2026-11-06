package com.educenter.service;

import com.educenter.dto.TeacherDTO;
import com.educenter.entity.Teacher;

import java.util.List;
import java.util.Optional;

public interface TeacherService {
    List<Teacher> findAll();
    List<Teacher> findAllActive();
    Optional<Teacher> findById(Long id);
    Teacher createTeacher(TeacherDTO dto);
    Teacher updateTeacher(Long id, TeacherDTO dto);
    void toggleActive(Long id);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
