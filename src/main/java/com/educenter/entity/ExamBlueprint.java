package com.educenter.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "exam_blueprints")
public class ExamBlueprint {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "owner_teacher_id", nullable = false) private Teacher ownerTeacher;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "subject_id", nullable = false) private Subject subject;
    @Column(name = "grade_level", nullable = false, length = 20) private String gradeLevel;
    @Column(nullable = false, length = 150) private String title;
    @Column(columnDefinition = "TEXT") private String scope;
    @Column(name = "preserve_question_order", nullable = false) private boolean preserveQuestionOrder;
    @Column(name = "duration_minutes", nullable = false) private int durationMinutes;
    @Column(name = "total_points", nullable = false, precision = 8, scale = 2) private BigDecimal totalPoints = BigDecimal.TEN;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private LocalDateTime updatedAt;
    public Long getId() { return id; }
    public Teacher getOwnerTeacher() { return ownerTeacher; }
    public void setOwnerTeacher(Teacher value) { ownerTeacher = value; }
    public Subject getSubject() { return subject; }
    public void setSubject(Subject value) { subject = value; }
    public String getGradeLevel() { return gradeLevel; }
    public void setGradeLevel(String value) { gradeLevel = value; }
    public String getTitle() { return title; }
    public void setTitle(String value) { title = value; }
    public String getScope() { return scope; }
    public void setScope(String value) { scope = value; }
    public boolean isPreserveQuestionOrder() { return preserveQuestionOrder; }
    public void setPreserveQuestionOrder(boolean value) { preserveQuestionOrder = value; }
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int value) { durationMinutes = value; }
    public BigDecimal getTotalPoints() { return totalPoints; }
    public void setTotalPoints(BigDecimal value) { totalPoints = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
