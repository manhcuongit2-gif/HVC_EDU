package com.educenter.repository;

import com.educenter.entity.HomeworkSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HomeworkSubmissionRepository extends JpaRepository<HomeworkSubmission, Long> {
    List<HomeworkSubmission> findByAssignmentIdOrderBySubmittedAtDesc(Long assignmentId);
    Optional<HomeworkSubmission> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId);
    Optional<HomeworkSubmission> findByIdAndAssignmentId(Long id, Long assignmentId);
    List<HomeworkSubmission> findByStudentParentUserIdAndAssignmentIdIn(Long parentUserId, List<Long> assignmentIds);
    List<HomeworkSubmission> findByStudentIdAndAssignmentIdIn(Long studentId, List<Long> assignmentIds);
}
