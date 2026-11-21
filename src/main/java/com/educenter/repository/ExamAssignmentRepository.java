package com.educenter.repository;

import com.educenter.entity.ExamAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface ExamAssignmentRepository extends JpaRepository<ExamAssignment, Long> {
    List<ExamAssignment> findByCreatedByIdOrderByCreatedAtDesc(Long userId);
    List<ExamAssignment> findByClassEntityIdOrderByOpensAtDesc(Long classId);
    Optional<ExamAssignment> findByIdAndCreatedById(Long id, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from ExamAssignment a where a.id = :id")
    Optional<ExamAssignment> findByIdForUpdate(@Param("id") Long id);
}
