package com.educenter.dto;

import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DirectExamForm {
    private String title;
    private Long classId;
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") private LocalDateTime opensAt;
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") private LocalDateTime closesAt;
    private Integer durationMinutes = 60;
    private Integer maxAttempts = 1;
    private boolean shuffleQuestions;
    private boolean shuffleOptions;
    private List<DirectExamQuestionForm> questions = new ArrayList<>();
    public String getTitle() { return title; }
    public void setTitle(String value) { title = value; }
    public Long getClassId() { return classId; }
    public void setClassId(Long value) { classId = value; }
    public LocalDateTime getOpensAt() { return opensAt; }
    public void setOpensAt(LocalDateTime value) { opensAt = value; }
    public LocalDateTime getClosesAt() { return closesAt; }
    public void setClosesAt(LocalDateTime value) { closesAt = value; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer value) { durationMinutes = value; }
    public Integer getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(Integer value) { maxAttempts = value; }
    public boolean isShuffleQuestions() { return shuffleQuestions; }
    public void setShuffleQuestions(boolean value) { shuffleQuestions = value; }
    public boolean isShuffleOptions() { return shuffleOptions; }
    public void setShuffleOptions(boolean value) { shuffleOptions = value; }
    public List<DirectExamQuestionForm> getQuestions() { return questions; }
    public void setQuestions(List<DirectExamQuestionForm> value) { questions = value; }
}
