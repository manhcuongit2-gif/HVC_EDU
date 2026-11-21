package com.educenter.dto;

import java.math.BigDecimal;

public record ExamQuestionAccuracy(String group, long correct, long responses, BigDecimal accuracyPercent) { }
