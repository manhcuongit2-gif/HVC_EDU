package com.educenter.service;

import com.educenter.dto.ClassDTO;
import com.educenter.entity.ClassEntity;
import com.educenter.entity.ClassStudent;

import java.util.List;

public interface ClassService {
    List<ClassEntity> getAllClasses();
    List<ClassEntity> getActiveClasses();
    ClassEntity getClassById(Long id);
    ClassEntity saveClass(ClassDTO classDTO);
    ClassEntity updateClass(Long id, ClassDTO classDTO);
    void deleteClass(Long id);
    long getStudentCountInClass(Long classId);
    List<ClassStudent> getStudentsInClass(Long classId);
    void addStudentToClass(Long classId, Long studentId);
    void removeStudentFromClass(Long classId, Long studentId);
    boolean isClassNameTaken(String name, Long excludeId);
}
