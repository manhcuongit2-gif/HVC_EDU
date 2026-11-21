package com.educenter.repository;

import com.educenter.entity.ExamAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Long> {
    List<ExamAttempt> findByAssignmentIdAndStudentIdOrderByAttemptNumberDesc(Long assignmentId, Long studentId);
    List<ExamAttempt> findByStudentIdOrderByStartedAtDesc(Long studentId);
    List<ExamAttempt> findByStudentIdIn(java.util.Collection<Long> studentIds);
    List<ExamAttempt> findByAssignmentIdOrderByAttemptNumberAsc(Long assignmentId);
    Optional<ExamAttempt> findFirstByAssignmentIdAndStudentIdAndStatusOrderByAttemptNumberDesc(Long assignmentId, Long studentId, String status);
    List<ExamAttempt> findByStatusAndDeadlineAtLessThanEqual(String status, LocalDateTime now);
}
