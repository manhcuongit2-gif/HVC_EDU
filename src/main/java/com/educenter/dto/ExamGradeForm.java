package com.educenter.dto;

import java.math.BigDecimal;

public class ExamGradeForm {
    private BigDecimal score;
    private String feedback;
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal value) { score = value; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String value) { feedback = value; }
}
