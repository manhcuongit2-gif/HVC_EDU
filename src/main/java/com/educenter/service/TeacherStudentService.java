package com.educenter.service;

import com.educenter.dto.TeacherStudentForm;
import com.educenter.entity.ClassEntity;
import com.educenter.entity.ClassStudent;
import com.educenter.entity.Student;

import java.util.List;

public interface TeacherStudentService {
    ClassEntity getClassForTeacher(Long teacherUserId, Long classId);
    List<ClassStudent> getRoster(Long teacherUserId, Long classId);
    List<Student> getAvailableStudents(Long teacherUserId, Long classId);
    void addExisting(Long teacherUserId, Long classId, Long studentId);
    void createAndEnroll(Long teacherUserId, Long classId, TeacherStudentForm form);
    Student getStudentForEdit(Long teacherUserId, Long classId, Long studentId);
    void updateStudent(Long teacherUserId, Long classId, Long studentId, TeacherStudentForm form);
    void dropFromClass(Long teacherUserId, Long classId, Long studentId);
}
