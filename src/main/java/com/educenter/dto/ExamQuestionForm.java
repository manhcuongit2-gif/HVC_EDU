package com.educenter.dto;

import java.math.BigDecimal;

public class ExamQuestionForm {
    private Long id;
    private Long subjectId;
    private String gradeLevel;
    private String topic;
    private String questionType = "MULTIPLE_CHOICE";
    private String difficulty = "MEDIUM";
    private String prompt;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String correctKey;
    private String acceptedAnswers;
    private String explanation;
    private BigDecimal defaultPoints = BigDecimal.ONE;
    public Long getId() { return id; }
    public void setId(Long value) { id = value; }
    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long value) { subjectId = value; }
    public String getGradeLevel() { return gradeLevel; }
    public void setGradeLevel(String value) { gradeLevel = value; }
    public String getTopic() { return topic; }
    public void setTopic(String value) { topic = value; }
    public String getQuestionType() { return questionType; }
    public void setQuestionType(String value) { questionType = value; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String value) { difficulty = value; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String value) { prompt = value; }
    public String getOptionA() { return optionA; }
    public void setOptionA(String value) { optionA = value; }
    public String getOptionB() { return optionB; }
    public void setOptionB(String value) { optionB = value; }
    public String getOptionC() { return optionC; }
    public void setOptionC(String value) { optionC = value; }
    public String getOptionD() { return optionD; }
    public void setOptionD(String value) { optionD = value; }
    public String getCorrectKey() { return correctKey; }
    public void setCorrectKey(String value) { correctKey = value; }
    public String getAcceptedAnswers() { return acceptedAnswers; }
    public void setAcceptedAnswers(String value) { acceptedAnswers = value; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String value) { explanation = value; }
    public BigDecimal getDefaultPoints() { return defaultPoints; }
    public void setDefaultPoints(BigDecimal value) { defaultPoints = value; }
}
