package com.educenter.dto;

import java.math.BigDecimal;
import java.util.List;

public record TeacherReviewQuestionView(int questionNumber, String type, String prompt, BigDecimal points,
                                       List<StudentExamOptionView> options, String expectedAnswer,
                                       String answerText, String selectedOptionKey, Boolean correct,
                                       BigDecimal score, String feedback) { }
