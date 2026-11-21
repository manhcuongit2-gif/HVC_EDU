package com.educenter.dto;

import java.time.LocalDateTime;
import java.util.List;

public record StudentExamAttemptView(Long assignmentId, Long attemptId, String title, String subject, String scope,
                                     String status, boolean autoSubmitted, LocalDateTime deadlineAt, LocalDateTime serverTime,
                                     long deadlineEpochMillis, long serverEpochMillis,
                                     List<StudentExamQuestionView> questions) { }
