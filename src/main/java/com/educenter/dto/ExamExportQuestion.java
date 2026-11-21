package com.educenter.dto;

import java.math.BigDecimal;
import java.util.List;

public record ExamExportQuestion(int number, String type, String prompt, BigDecimal points,
                                 List<ExamExportOption> options, String answer, String explanation) {
    public record ExamExportOption(String key, String text) { }
}
