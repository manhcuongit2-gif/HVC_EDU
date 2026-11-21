package com.educenter.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "exam_attempts", uniqueConstraints = @UniqueConstraint(name = "uk_exam_attempt_number", columnNames = {"assignment_id", "student_id", "attempt_number"}))
public class ExamAttempt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "assignment_id", nullable = false) private ExamAssignment assignment;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "student_id", nullable = false) private Student student;
    @Column(name = "attempt_number", nullable = false) private int attemptNumber;
    @Column(nullable = false, length = 24) private String status;
    @Column(name = "started_at", nullable = false) private LocalDateTime startedAt;
    @Column(name = "deadline_at", nullable = false) private LocalDateTime deadlineAt;
    @Column(name = "submitted_at") private LocalDateTime submittedAt;
    @Column(name = "auto_submitted", nullable = false) private boolean autoSubmitted;
    @Column(name = "question_order_json", nullable = false, columnDefinition = "LONGTEXT") private String questionOrderJson;
    @Column(name = "option_order_json", nullable = false, columnDefinition = "LONGTEXT") private String optionOrderJson;
    @Column(name = "objective_score", nullable = false, precision = 8, scale = 2) private BigDecimal objectiveScore = BigDecimal.ZERO;
    @Column(name = "manual_score", nullable = false, precision = 8, scale = 2) private BigDecimal manualScore = BigDecimal.ZERO;
    @Column(name = "total_score", precision = 8, scale = 2) private BigDecimal totalScore;
    @Version private long version;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private LocalDateTime updatedAt;
    public Long getId() { return id; }
    public ExamAssignment getAssignment() { return assignment; }
    public void setAssignment(ExamAssignment value) { assignment = value; }
    public Student getStudent() { return student; }
    public void setStudent(Student value) { student = value; }
    public int getAttemptNumber() { return attemptNumber; }
    public void setAttemptNumber(int value) { attemptNumber = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime value) { startedAt = value; }
    public LocalDateTime getDeadlineAt() { return deadlineAt; }
    public void setDeadlineAt(LocalDateTime value) { deadlineAt = value; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime value) { submittedAt = value; }
    public boolean isAutoSubmitted() { return autoSubmitted; }
    public void setAutoSubmitted(boolean value) { autoSubmitted = value; }
    public String getQuestionOrderJson() { return questionOrderJson; }
    public void setQuestionOrderJson(String value) { questionOrderJson = value; }
    public String getOptionOrderJson() { return optionOrderJson; }
    public void setOptionOrderJson(String value) { optionOrderJson = value; }
    public BigDecimal getObjectiveScore() { return objectiveScore; }
    public void setObjectiveScore(BigDecimal value) { objectiveScore = value; }
    public BigDecimal getManualScore() { return manualScore; }
    public void setManualScore(BigDecimal value) { manualScore = value; }
    public BigDecimal getTotalScore() { return totalScore; }
    public void setTotalScore(BigDecimal value) { totalScore = value; }
}
