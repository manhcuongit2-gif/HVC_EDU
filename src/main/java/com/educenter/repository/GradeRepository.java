package com.educenter.repository;

import com.educenter.entity.Grade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GradeRepository extends JpaRepository<Grade, Long> {
    List<Grade> findByClassEntityId(Long classId);
    List<Grade> findByStudentId(Long studentId);
    Optional<Grade> findByClassEntityIdAndStudentId(Long classId, Long studentId);
}
