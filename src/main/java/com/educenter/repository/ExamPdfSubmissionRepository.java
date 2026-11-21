package com.educenter.repository;

import com.educenter.entity.ExamPdfSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ExamPdfSubmissionRepository extends JpaRepository<ExamPdfSubmission, Long> {
    List<ExamPdfSubmission> findByAssignmentIdOrderBySubmittedAtDesc(Long assignmentId);
    List<ExamPdfSubmission> findByAssignmentIdAndStudentIdOrderByVersionNumberDesc(Long assignmentId, Long studentId);
    Optional<ExamPdfSubmission> findFirstByAssignmentIdAndStudentIdOrderByVersionNumberDesc(Long assignmentId, Long studentId);
    List<ExamPdfSubmission> findByStudentIdOrderBySubmittedAtDesc(Long studentId);
}
