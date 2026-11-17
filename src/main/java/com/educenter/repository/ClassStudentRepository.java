package com.educenter.repository;

import com.educenter.entity.ClassStudent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassStudentRepository extends JpaRepository<ClassStudent, Long> {
    List<ClassStudent> findByClassEntityId(Long classId);
    List<ClassStudent> findByClassEntityIdAndStatus(Long classId, String status);
    List<ClassStudent> findByClassEntityIdOrderByStudentFullNameAsc(Long classId);
    List<ClassStudent> findByStudentId(Long studentId);
    List<ClassStudent> findByStudentParentUserId(Long userId);
    long countByClassEntityIdAndStatus(Long classId, String status);
    boolean existsByClassEntityIdAndStudentId(Long classId, Long studentId);
    Optional<ClassStudent> findByClassEntityIdAndStudentId(Long classId, Long studentId);
    void deleteByClassEntityIdAndStudentId(Long classId, Long studentId);
}
