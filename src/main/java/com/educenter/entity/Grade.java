package com.educenter.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "grades", uniqueConstraints = {
    @UniqueConstraint(name = "uk_grade_class_student", columnNames = {"class_id", "student_id"})
})
public class Grade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "class_id", nullable = false)
    private ClassEntity classEntity;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "test_score", precision = 4, scale = 2)
    @DecimalMin(value = "0.0", message = "Điểm phải từ 0 đến 10")
    @DecimalMax(value = "10.0", message = "Điểm phải từ 0 đến 10")
    private BigDecimal testScore;

    @Column(name = "midterm_score", precision = 4, scale = 2)
    @DecimalMin(value = "0.0", message = "Điểm phải từ 0 đến 10")
    @DecimalMax(value = "10.0", message = "Điểm phải từ 0 đến 10")
    private BigDecimal midtermScore;

    @Column(name = "final_score", precision = 4, scale = 2)
    @DecimalMin(value = "0.0", message = "Điểm phải từ 0 đến 10")
    @DecimalMax(value = "10.0", message = "Điểm phải từ 0 đến 10")
    private BigDecimal finalScore;

    @Column(name = "average_score", precision = 4, scale = 2)
    private BigDecimal averageScore;

    @Column(name = "note", length = 500)
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Grade() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ClassEntity getClassEntity() { return classEntity; }
    public void setClassEntity(ClassEntity classEntity) { this.classEntity = classEntity; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public BigDecimal getTestScore() { return testScore; }
    public void setTestScore(BigDecimal testScore) { this.testScore = testScore; }

    public BigDecimal getMidtermScore() { return midtermScore; }
    public void setMidtermScore(BigDecimal midtermScore) { this.midtermScore = midtermScore; }

    public BigDecimal getFinalScore() { return finalScore; }
    public void setFinalScore(BigDecimal finalScore) { this.finalScore = finalScore; }

    public BigDecimal getAverageScore() { return averageScore; }
    public void setAverageScore(BigDecimal averageScore) { this.averageScore = averageScore; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public void recalculateAverageScore() {
        if (testScore != null && midtermScore != null && finalScore != null) {
            BigDecimal weightedSum = testScore.multiply(BigDecimal.valueOf(1))
                    .add(midtermScore.multiply(BigDecimal.valueOf(2)))
                    .add(finalScore.multiply(BigDecimal.valueOf(3)));
            this.averageScore = weightedSum.divide(BigDecimal.valueOf(6), 2, RoundingMode.HALF_UP);
        } else {
            this.averageScore = null;
        }
    }

    public String getAcademicPerformance() {
        if (averageScore == null) return "Chưa đủ cột điểm";
        double avg = averageScore.doubleValue();
        if (avg >= 8.0) return "Giỏi";
        if (avg >= 6.5) return "Khá";
        if (avg >= 5.0) return "Trung bình";
        return "Yếu";
    }
}
