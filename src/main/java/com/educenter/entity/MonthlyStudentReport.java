package com.educenter.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "monthly_student_reports", uniqueConstraints = {
        @UniqueConstraint(name = "uk_monthly_student_report", columnNames = {"class_id", "student_id", "report_month"})
})
public class MonthlyStudentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "class_id", nullable = false)
    private ClassEntity classEntity;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "report_month", nullable = false)
    private LocalDate reportMonth;

    @Column(name = "present_count", nullable = false)
    private Integer presentCount = 0;

    @Column(name = "absent_count", nullable = false)
    private Integer absentCount = 0;

    @Column(name = "excused_count", nullable = false)
    private Integer excusedCount = 0;

    @Column(name = "homework_average", precision = 4, scale = 2)
    private BigDecimal homeworkAverage;

    @Column(name = "good_evaluation_count", nullable = false)
    private Integer goodEvaluationCount = 0;

    @Column(name = "teacher_comment", length = 1000, nullable = false)
    private String teacherComment;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "published_by")
    private User publishedBy;

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
    public ClassEntity getClassEntity() { return classEntity; }
    public void setClassEntity(ClassEntity classEntity) { this.classEntity = classEntity; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public LocalDate getReportMonth() { return reportMonth; }
    public void setReportMonth(LocalDate reportMonth) { this.reportMonth = reportMonth; }
    public Integer getPresentCount() { return presentCount; }
    public void setPresentCount(Integer presentCount) { this.presentCount = presentCount; }
    public Integer getAbsentCount() { return absentCount; }
    public void setAbsentCount(Integer absentCount) { this.absentCount = absentCount; }
    public Integer getExcusedCount() { return excusedCount; }
    public void setExcusedCount(Integer excusedCount) { this.excusedCount = excusedCount; }
    public BigDecimal getHomeworkAverage() { return homeworkAverage; }
    public void setHomeworkAverage(BigDecimal homeworkAverage) { this.homeworkAverage = homeworkAverage; }
    public Integer getGoodEvaluationCount() { return goodEvaluationCount; }
    public void setGoodEvaluationCount(Integer goodEvaluationCount) { this.goodEvaluationCount = goodEvaluationCount; }
    public String getTeacherComment() { return teacherComment; }
    public void setTeacherComment(String teacherComment) { this.teacherComment = teacherComment; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
    public User getPublishedBy() { return publishedBy; }
    public void setPublishedBy(User publishedBy) { this.publishedBy = publishedBy; }
    public User getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(User updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
