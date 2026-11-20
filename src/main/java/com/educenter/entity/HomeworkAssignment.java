package com.educenter.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "homework_assignments")
public class HomeworkAssignment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "class_id", nullable = false) private ClassEntity classEntity;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by", nullable = false) private User createdBy;
    @Column(nullable = false, length = 150) private String title;
    @Lob @Column(nullable = false) private String instructions;
    @Column(name = "due_at", nullable = false) private LocalDateTime dueAt;
    @Column(name = "attachment_stored_filename", length = 100) private String attachmentStoredFilename;
    @Column(name = "attachment_original_filename", length = 255) private String attachmentOriginalFilename;
    @Column(name = "attachment_content_type", length = 100) private String attachmentContentType;
    @Column(name = "attachment_file_size") private Long attachmentFileSize;
    @Column(name = "published_at", nullable = false) private LocalDateTime publishedAt;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public ClassEntity getClassEntity() { return classEntity; }
    public void setClassEntity(ClassEntity classEntity) { this.classEntity = classEntity; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    public LocalDateTime getDueAt() { return dueAt; }
    public void setDueAt(LocalDateTime dueAt) { this.dueAt = dueAt; }
    public String getAttachmentStoredFilename() { return attachmentStoredFilename; }
    public void setAttachmentStoredFilename(String value) { this.attachmentStoredFilename = value; }
    public String getAttachmentOriginalFilename() { return attachmentOriginalFilename; }
    public void setAttachmentOriginalFilename(String value) { this.attachmentOriginalFilename = value; }
    public String getAttachmentContentType() { return attachmentContentType; }
    public void setAttachmentContentType(String value) { this.attachmentContentType = value; }
    public Long getAttachmentFileSize() { return attachmentFileSize; }
    public void setAttachmentFileSize(Long value) { this.attachmentFileSize = value; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
