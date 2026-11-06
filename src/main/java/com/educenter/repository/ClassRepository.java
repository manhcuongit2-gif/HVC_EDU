package com.educenter.repository;

import com.educenter.entity.ClassEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassRepository extends JpaRepository<ClassEntity, Long> {
    List<ClassEntity> findAllByOrderByIdDesc();
    List<ClassEntity> findByStatus(String status);
    List<ClassEntity> findByTeacherId(Long teacherId);
    List<ClassEntity> findByTeacherUserId(Long userId);
    List<ClassEntity> findBySubjectId(Long subjectId);
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);
}
