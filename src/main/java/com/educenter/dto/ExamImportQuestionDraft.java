package com.educenter.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ExamImportQuestionDraft {
    private String prompt;
    private String topic;
    private String questionType;
    private String difficulty;
    private BigDecimal points;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String correctKey;
    private String sourceAnswer;
    private String acceptedAnswers;
    private String explanation;
    private List<String> needsReview = new ArrayList<>();

    public String getPrompt() { return prompt; }
    public void setPrompt(String value) { prompt = value; }
    public String getTopic() { return topic; }
    public void setTopic(String value) { topic = value; }
    public String getQuestionType() { return questionType; }
    public void setQuestionType(String value) { questionType = value; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String value) { difficulty = value; }
    public BigDecimal getPoints() { return points; }
    public void setPoints(BigDecimal value) { points = value; }
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
    public String getSourceAnswer() { return sourceAnswer; }
    public void setSourceAnswer(String value) { sourceAnswer = value; }
    public String getAcceptedAnswers() { return acceptedAnswers; }
    public void setAcceptedAnswers(String value) { acceptedAnswers = value; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String value) { explanation = value; }
    public List<String> getNeedsReview() { return needsReview; }
    public void setNeedsReview(List<String> value) { needsReview = value == null ? new ArrayList<>() : value; }
}
