package com.educenter.dto;

import java.math.BigDecimal;

public class ExamMatrixCellForm {
    private String topic;
    private String questionType;
    private String difficulty;
    private Integer questionCount;
    private BigDecimal cellPoints;
    public String getTopic() { return topic; }
    public void setTopic(String value) { topic = value; }
    public String getQuestionType() { return questionType; }
    public void setQuestionType(String value) { questionType = value; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String value) { difficulty = value; }
    public Integer getQuestionCount() { return questionCount; }
    public void setQuestionCount(Integer value) { questionCount = value; }
    public BigDecimal getCellPoints() { return cellPoints; }
    public void setCellPoints(BigDecimal value) { cellPoints = value; }
}
