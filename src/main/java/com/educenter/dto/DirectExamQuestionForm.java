package com.educenter.dto;

import java.math.BigDecimal;

public class DirectExamQuestionForm {
    private String type = "MULTIPLE_CHOICE";
    private String prompt;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String correctKey;
    private String acceptedAnswers;
    private BigDecimal points = BigDecimal.ONE;
    public String getType() { return type; }
    public void setType(String value) { type = value; }
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
    public BigDecimal getPoints() { return points; }
    public void setPoints(BigDecimal value) { points = value; }
}
