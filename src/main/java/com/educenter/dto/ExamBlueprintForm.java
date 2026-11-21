package com.educenter.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ExamBlueprintForm {
    private String title;
    private Long subjectId;
    private String gradeLevel;
    private String scope;
    private Integer durationMinutes = 60;
    private BigDecimal totalPoints = BigDecimal.TEN;
    private List<ExamMatrixCellForm> cells = new ArrayList<>();
    public String getTitle() { return title; }
    public void setTitle(String value) { title = value; }
    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long value) { subjectId = value; }
    public String getGradeLevel() { return gradeLevel; }
    public void setGradeLevel(String value) { gradeLevel = value; }
    public String getScope() { return scope; }
    public void setScope(String value) { scope = value; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer value) { durationMinutes = value; }
    public BigDecimal getTotalPoints() { return totalPoints; }
    public void setTotalPoints(BigDecimal value) { totalPoints = value; }
    public List<ExamMatrixCellForm> getCells() { return cells; }
    public void setCells(List<ExamMatrixCellForm> value) { cells = value; }
}
