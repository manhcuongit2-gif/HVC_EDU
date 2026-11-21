package com.educenter.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ExamImportSaveForm {
    private Long blueprintId;
    private String mode;
    private String sourceFilename;
    private String sourceSubjectName;
    private String title;
    private Long subjectId;
    private String gradeLevel;
    private String scope;
    private Integer durationMinutes;
    private BigDecimal totalPoints;
    private boolean totalPointsDerived;
    private List<String> warnings = new ArrayList<>();
    private List<ExamImportQuestionDraft> questions = new ArrayList<>();

    public Long getBlueprintId() { return blueprintId; }
    public void setBlueprintId(Long value) { blueprintId = value; }
    public String getMode() { return mode; }
    public void setMode(String value) { mode = value; }
    public String getSourceFilename() { return sourceFilename; }
    public void setSourceFilename(String value) { sourceFilename = value; }
    public String getSourceSubjectName() { return sourceSubjectName; }
    public void setSourceSubjectName(String value) { sourceSubjectName = value; }
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
    public boolean isTotalPointsDerived() { return totalPointsDerived; }
    public void setTotalPointsDerived(boolean value) { totalPointsDerived = value; }
    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> value) { warnings = value == null ? new ArrayList<>() : value; }
    public List<ExamImportQuestionDraft> getQuestions() { return questions; }
    public void setQuestions(List<ExamImportQuestionDraft> value) { questions = value == null ? new ArrayList<>() : value; }
}
