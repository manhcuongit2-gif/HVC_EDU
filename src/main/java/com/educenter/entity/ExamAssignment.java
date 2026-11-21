package com.educenter.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "exam_assignments")
public class ExamAssignment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "blueprint_id") private ExamBlueprint blueprint;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "class_id", nullable = false) private ClassEntity classEntity;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by", nullable = false) private User createdBy;
    @Column(nullable = false, length = 150) private String title;
    @Column(name = "exam_type", nullable = false, length = 16) private String examType = "ONLINE";
    @Column(name = "pdf_stored_filename", length = 80) private String pdfStoredFilename;
    @Column(name = "pdf_original_filename", length = 255) private String pdfOriginalFilename;
    @Column(name = "pdf_content_type", length = 100) private String pdfContentType;
    @Column(name = "pdf_file_size") private Long pdfFileSize;
    @Column(columnDefinition = "TEXT") private String scope;
    @Column(name = "opens_at", nullable = false) private LocalDateTime opensAt;
    @Column(name = "closes_at", nullable = false) private LocalDateTime closesAt;
    @Column(name = "duration_minutes", nullable = false) private int durationMinutes;
    @Column(name = "max_attempts", nullable = false) private int maxAttempts = 1;
    @Column(name = "shuffle_questions", nullable = false) private boolean shuffleQuestions = true;
    @Column(name = "shuffle_options", nullable = false) private boolean shuffleOptions = true;
    @Column(name = "total_points", nullable = false, precision = 8, scale = 2) private BigDecimal totalPoints;
    @Column(name = "questions_snapshot_json", nullable = false, columnDefinition = "LONGTEXT") private String questionsSnapshotJson;
    @Column(name = "result_published_at") private LocalDateTime resultPublishedAt;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    public Long getId() { return id; }
    public ExamBlueprint getBlueprint() { return blueprint; }
    public void setBlueprint(ExamBlueprint value) { blueprint = value; }
    public ClassEntity getClassEntity() { return classEntity; }
    public void setClassEntity(ClassEntity value) { classEntity = value; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User value) { createdBy = value; }
    public String getTitle() { return title; }
    public void setTitle(String value) { title = value; }
    public String getExamType() { return examType; }
    public void setExamType(String value) { examType = value; }
    public boolean isPdfExam() { return "PDF".equals(examType); }
    public String getPdfStoredFilename() { return pdfStoredFilename; }
    public void setPdfStoredFilename(String value) { pdfStoredFilename = value; }
    public String getPdfOriginalFilename() { return pdfOriginalFilename; }
    public void setPdfOriginalFilename(String value) { pdfOriginalFilename = value; }
    public String getPdfContentType() { return pdfContentType; }
    public void setPdfContentType(String value) { pdfContentType = value; }
    public Long getPdfFileSize() { return pdfFileSize; }
    public void setPdfFileSize(Long value) { pdfFileSize = value; }
    public String getScope() { return scope; }
    public void setScope(String value) { scope = value; }
    public LocalDateTime getOpensAt() { return opensAt; }
    public void setOpensAt(LocalDateTime value) { opensAt = value; }
    public LocalDateTime getClosesAt() { return closesAt; }
    public void setClosesAt(LocalDateTime value) { closesAt = value; }
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int value) { durationMinutes = value; }
    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int value) { maxAttempts = value; }
    public boolean isShuffleQuestions() { return shuffleQuestions; }
    public void setShuffleQuestions(boolean value) { shuffleQuestions = value; }
    public boolean isShuffleOptions() { return shuffleOptions; }
    public void setShuffleOptions(boolean value) { shuffleOptions = value; }
    public BigDecimal getTotalPoints() { return totalPoints; }
    public void setTotalPoints(BigDecimal value) { totalPoints = value; }
    public String getQuestionsSnapshotJson() { return questionsSnapshotJson; }
    public void setQuestionsSnapshotJson(String value) { questionsSnapshotJson = value; }
    public LocalDateTime getResultPublishedAt() { return resultPublishedAt; }
    public void setResultPublishedAt(LocalDateTime value) { resultPublishedAt = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
