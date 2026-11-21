package com.educenter.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "exam_pdf_submissions", uniqueConstraints =
        @UniqueConstraint(name = "uk_exam_pdf_submission_version", columnNames = {"assignment_id", "student_id", "version_number"}))
public class ExamPdfSubmission {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "assignment_id", nullable = false) private ExamAssignment assignment;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "student_id", nullable = false) private Student student;
    @Column(name = "version_number", nullable = false) private int versionNumber;
    @Column(nullable = false, length = 20) private String status;
    @Column(precision = 5, scale = 2) private BigDecimal score;
    @Column(length = 1000) private String feedback;
    @Column(name = "submitted_at", nullable = false) private LocalDateTime submittedAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reviewed_by") private User reviewedBy;
    @Column(name = "reviewed_at") private LocalDateTime reviewedAt;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC") private List<ExamPdfSubmissionFile> files = new ArrayList<>();

    public Long getId() { return id; }
    public ExamAssignment getAssignment() { return assignment; }
    public void setAssignment(ExamAssignment value) { assignment = value; }
    public Student getStudent() { return student; }
    public void setStudent(Student value) { student = value; }
    public int getVersionNumber() { return versionNumber; }
    public void setVersionNumber(int value) { versionNumber = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal value) { score = value; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String value) { feedback = value; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime value) { submittedAt = value; }
    public User getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(User value) { reviewedBy = value; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime value) { reviewedAt = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<ExamPdfSubmissionFile> getFiles() { return files; }
    public void addFile(ExamPdfSubmissionFile file) { files.add(file); file.setSubmission(this); }
}
