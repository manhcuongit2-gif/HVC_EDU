package com.educenter.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "exam_pdf_submission_files")
public class ExamPdfSubmissionFile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "submission_id", nullable = false) private ExamPdfSubmission submission;
    @Column(name = "stored_filename", nullable = false, length = 80) private String storedFilename;
    @Column(name = "original_filename", nullable = false, length = 255) private String originalFilename;
    @Column(name = "content_type", nullable = false, length = 100) private String contentType;
    @Column(name = "file_size", nullable = false) private long fileSize;
    @Column(nullable = false) private int position;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    public Long getId() { return id; }
    public ExamPdfSubmission getSubmission() { return submission; }
    public void setSubmission(ExamPdfSubmission value) { submission = value; }
    public String getStoredFilename() { return storedFilename; }
    public void setStoredFilename(String value) { storedFilename = value; }
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String value) { originalFilename = value; }
    public String getContentType() { return contentType; }
    public void setContentType(String value) { contentType = value; }
    public long getFileSize() { return fileSize; }
    public void setFileSize(long value) { fileSize = value; }
    public int getPosition() { return position; }
    public void setPosition(int value) { position = value; }
}
