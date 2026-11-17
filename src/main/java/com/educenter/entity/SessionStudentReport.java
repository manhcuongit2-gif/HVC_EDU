package com.educenter.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "session_student_reports", uniqueConstraints = {
        @UniqueConstraint(name = "uk_session_student_report", columnNames = {"session_id", "student_id"})
})
public class SessionStudentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "session_id", nullable = false)
    private ClassSession classSession;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "attendance_status", length = 20, nullable = false)
    private String attendanceStatus;

    @Column(name = "homework_score", precision = 4, scale = 2)
    private BigDecimal homeworkScore;

    @Column(name = "comment", length = 1000, nullable = false)
    private String comment;

    @Column(name = "good_evaluation_count", nullable = false)
    private Integer goodEvaluationCount = 0;

    @Column(name = "note", length = 500)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ClassSession getClassSession() { return classSession; }
    public void setClassSession(ClassSession classSession) { this.classSession = classSession; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public String getAttendanceStatus() { return attendanceStatus; }
    public void setAttendanceStatus(String attendanceStatus) { this.attendanceStatus = attendanceStatus; }
    public BigDecimal getHomeworkScore() { return homeworkScore; }
    public void setHomeworkScore(BigDecimal homeworkScore) { this.homeworkScore = homeworkScore; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public Integer getGoodEvaluationCount() { return goodEvaluationCount; }
    public void setGoodEvaluationCount(Integer goodEvaluationCount) { this.goodEvaluationCount = goodEvaluationCount; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public User getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(User updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
