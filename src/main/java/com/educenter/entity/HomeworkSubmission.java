package com.educenter.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "homework_submissions", uniqueConstraints = @UniqueConstraint(name = "uk_homework_assignment_student", columnNames = {"assignment_id", "student_id"}))
public class HomeworkSubmission {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "assignment_id", nullable = false) private HomeworkAssignment assignment;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "student_id", nullable = false) private Student student;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "submitted_by", nullable = false) private User submittedBy;
    @Column(name = "submission_text", columnDefinition = "TEXT") private String submissionText;
    @Column(name = "attachment_stored_filename", length = 100) private String attachmentStoredFilename;
    @Column(name = "attachment_original_filename", length = 255) private String attachmentOriginalFilename;
    @Column(name = "attachment_content_type", length = 100) private String attachmentContentType;
    @Column(name = "attachment_file_size") private Long attachmentFileSize;
    @Column(name = "submitted_at", nullable = false) private LocalDateTime submittedAt;
    @Column(nullable = false) private boolean late;
    @Column(nullable = false, length = 20) private String status = "SUBMITTED";
    @Column(precision = 4, scale = 2) private BigDecimal score;
    @Column(length = 1000) private String feedback;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reviewed_by") private User reviewedBy;
    @Column(name = "reviewed_at") private LocalDateTime reviewedAt;

    public Long getId() { return id; }
    public HomeworkAssignment getAssignment() { return assignment; }
    public void setAssignment(HomeworkAssignment assignment) { this.assignment = assignment; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public User getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(User submittedBy) { this.submittedBy = submittedBy; }
    public String getSubmissionText() { return submissionText; }
    public void setSubmissionText(String submissionText) { this.submissionText = submissionText; }
    public String getAttachmentStoredFilename() { return attachmentStoredFilename; }
    public void setAttachmentStoredFilename(String value) { this.attachmentStoredFilename = value; }
    public String getAttachmentOriginalFilename() { return attachmentOriginalFilename; }
    public void setAttachmentOriginalFilename(String value) { this.attachmentOriginalFilename = value; }
    public String getAttachmentContentType() { return attachmentContentType; }
    public void setAttachmentContentType(String value) { this.attachmentContentType = value; }
    public Long getAttachmentFileSize() { return attachmentFileSize; }
    public void setAttachmentFileSize(Long value) { this.attachmentFileSize = value; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public boolean isLate() { return late; }
    public void setLate(boolean late) { this.late = late; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
    public User getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(User reviewedBy) { this.reviewedBy = reviewedBy; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
}
