package com.educenter.dto;

import com.educenter.entity.Student;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record TeacherExamAttemptView(Long id, Student student, int attemptNumber, String status, boolean autoSubmitted,
                                     LocalDateTime startedAt, LocalDateTime submittedAt,
                                     BigDecimal totalScore, List<TeacherReviewQuestionView> questions) { }
