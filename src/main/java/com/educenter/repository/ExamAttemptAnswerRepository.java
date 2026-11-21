package com.educenter.repository;

import com.educenter.entity.ExamAttemptAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ExamAttemptAnswerRepository extends JpaRepository<ExamAttemptAnswer, Long> {
    List<ExamAttemptAnswer> findByAttemptIdOrderByQuestionNumberAsc(Long attemptId);
    Optional<ExamAttemptAnswer> findByAttemptIdAndQuestionNumber(Long attemptId, int questionNumber);
}
