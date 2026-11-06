package com.educenter.service;

import com.educenter.dto.StudentDTO;
import com.educenter.entity.Student;

import java.util.List;
import java.util.Optional;

public interface StudentService {
    List<Student> findAll();
    List<Student> findAllActive();
    List<Student> findByParentId(Long parentId);
    Optional<Student> findById(Long id);
    Student createStudent(StudentDTO dto);
    Student updateStudent(Long id, StudentDTO dto);
    void toggleActive(Long id);
}
