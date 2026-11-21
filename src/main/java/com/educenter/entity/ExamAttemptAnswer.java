package com.educenter.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "exam_attempt_answers", uniqueConstraints = @UniqueConstraint(name = "uk_exam_attempt_answer", columnNames = {"attempt_id", "question_number"}))
public class ExamAttemptAnswer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "attempt_id", nullable = false) private ExamAttempt attempt;
    @Column(name = "question_number", nullable = false) private int questionNumber;
    @Column(name = "answer_text", columnDefinition = "TEXT") private String answerText;
    @Column(name = "selected_option_key", length = 20) private String selectedOptionKey;
    @Column(name = "is_correct") private Boolean correct;
    @Column(precision = 8, scale = 2) private BigDecimal score;
    @Column(name = "teacher_feedback", length = 1000) private String teacherFeedback;
    @UpdateTimestamp @Column(name = "updated_at") private LocalDateTime updatedAt;
    public Long getId() { return id; }
    public ExamAttempt getAttempt() { return attempt; }
    public void setAttempt(ExamAttempt value) { attempt = value; }
    public int getQuestionNumber() { return questionNumber; }
    public void setQuestionNumber(int value) { questionNumber = value; }
    public String getAnswerText() { return answerText; }
    public void setAnswerText(String value) { answerText = value; }
    public String getSelectedOptionKey() { return selectedOptionKey; }
    public void setSelectedOptionKey(String value) { selectedOptionKey = value; }
    public Boolean getCorrect() { return correct; }
    public void setCorrect(Boolean value) { correct = value; }
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal value) { score = value; }
    public String getTeacherFeedback() { return teacherFeedback; }
    public void setTeacherFeedback(String value) { teacherFeedback = value; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
