package com.educenter.repository;

import com.educenter.entity.ExamQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ExamQuestionRepository extends JpaRepository<ExamQuestion, Long> {
    List<ExamQuestion> findByOwnerTeacherIdOrderByUpdatedAtDesc(Long ownerTeacherId);
    List<ExamQuestion> findByOwnerTeacherIdAndSubjectIdAndGradeLevelAndActiveTrue(Long ownerTeacherId, Long subjectId, String gradeLevel);
    Optional<ExamQuestion> findByIdAndOwnerTeacherId(Long id, Long ownerTeacherId);
}
