package com.educenter.dto;

import java.math.BigDecimal;
import java.util.List;

public record StudentExamQuestionView(int questionNumber, String type, String prompt, BigDecimal points,
                                     List<StudentExamOptionView> options, String answerText, String selectedOptionKey) { }
