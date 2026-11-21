package com.educenter.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "exam_questions")
public class ExamQuestion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "owner_teacher_id", nullable = false) private Teacher ownerTeacher;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "subject_id", nullable = false) private Subject subject;
    @Column(name = "grade_level", nullable = false, length = 20) private String gradeLevel;
    @Column(length = 150) private String topic;
    @Column(name = "question_type", nullable = false, length = 24) private String questionType;
    @Column(nullable = false, length = 20) private String difficulty;
    @Column(nullable = false, columnDefinition = "TEXT") private String prompt;
    @Column(name = "options_json", columnDefinition = "LONGTEXT") private String optionsJson;
    @Column(name = "answer_key_json", nullable = false, columnDefinition = "LONGTEXT") private String answerKeyJson;
    @Column(columnDefinition = "TEXT") private String explanation;
    @Column(name = "default_points", nullable = false, precision = 8, scale = 2) private BigDecimal defaultPoints = BigDecimal.ONE;
    @Column(nullable = false) private boolean active = true;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public Teacher getOwnerTeacher() { return ownerTeacher; }
    public void setOwnerTeacher(Teacher value) { ownerTeacher = value; }
    public Subject getSubject() { return subject; }
    public void setSubject(Subject value) { subject = value; }
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
    public String getOptionsJson() { return optionsJson; }
    public void setOptionsJson(String value) { optionsJson = value; }
    public String getAnswerKeyJson() { return answerKeyJson; }
    public void setAnswerKeyJson(String value) { answerKeyJson = value; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String value) { explanation = value; }
    public BigDecimal getDefaultPoints() { return defaultPoints; }
    public void setDefaultPoints(BigDecimal value) { defaultPoints = value; }
    public boolean isActive() { return active; }
    public void setActive(boolean value) { active = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
