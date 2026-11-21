package com.educenter.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_ai_study_plans")
public class StudentAIStudyPlan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false, unique = true)
    private Student student;

    @Column(name = "source_hash", nullable = false, length = 64)
    private String sourceHash;

    @CreationTimestamp @Column(name = "generated_at", nullable = false, updatable = false)
    private LocalDateTime generatedAt;

    @UpdateTimestamp @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public String getSourceHash() { return sourceHash; }
    public void setSourceHash(String sourceHash) { this.sourceHash = sourceHash; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
