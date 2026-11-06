package com.educenter.service.impl;

import com.educenter.dto.StudentDTO;
import com.educenter.entity.Parent;
import com.educenter.entity.Student;
import com.educenter.repository.ParentRepository;
import com.educenter.repository.StudentRepository;
import com.educenter.service.StudentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;

    public StudentServiceImpl(StudentRepository studentRepository,
                              ParentRepository parentRepository) {
        this.studentRepository = studentRepository;
        this.parentRepository = parentRepository;
    }

    @Override
    public List<Student> findAll() {
        return studentRepository.findAllByOrderByIdAsc();
    }

    @Override
    public List<Student> findAllActive() {
        return studentRepository.findByActiveTrue();
    }

    @Override
    public List<Student> findByParentId(Long parentId) {
        return studentRepository.findByParentId(parentId);
    }

    @Override
    public Optional<Student> findById(Long id) {
        return studentRepository.findById(id);
    }

    @Override
    @Transactional
    public Student createStudent(StudentDTO dto) {
        Parent parent = parentRepository.findById(dto.getParentId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phụ huynh với ID: " + dto.getParentId()));

        Student student = new Student();
        student.setFullName(dto.getFullName());
        student.setDateOfBirth(dto.getDateOfBirth());
        student.setGender(dto.getGender());
        student.setPhone(dto.getPhone());
        student.setSchool(dto.getSchool());
        student.setGradeLevel(dto.getGradeLevel());
        student.setParent(parent);
        student.setActive(dto.getActive() != null ? dto.getActive() : true);

        return studentRepository.save(student);
    }

    @Override
    @Transactional
    public Student updateStudent(Long id, StudentDTO dto) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học sinh với ID: " + id));

        Parent parent = parentRepository.findById(dto.getParentId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phụ huynh với ID: " + dto.getParentId()));

        student.setFullName(dto.getFullName());
        student.setDateOfBirth(dto.getDateOfBirth());
        student.setGender(dto.getGender());
        student.setPhone(dto.getPhone());
        student.setSchool(dto.getSchool());
        student.setGradeLevel(dto.getGradeLevel());
        student.setParent(parent);

        return studentRepository.save(student);
    }

    @Override
    @Transactional
    public void toggleActive(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học sinh với ID: " + id));
        student.setActive(!student.getActive());
        studentRepository.save(student);
    }
}
