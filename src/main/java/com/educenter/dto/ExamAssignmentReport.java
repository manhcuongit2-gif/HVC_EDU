package com.educenter.dto;

import com.educenter.entity.ExamAssignment;
import java.util.List;

public record ExamAssignmentReport(ExamAssignment assignment, List<TeacherExamAttemptView> attempts,
                                  List<ExamQuestionAccuracy> byTopic,
                                  List<ExamQuestionAccuracy> byType,
                                  List<ExamQuestionAccuracy> byDifficulty) { }
