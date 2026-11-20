package com.educenter.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_notifications", uniqueConstraints = @UniqueConstraint(name = "uk_user_notifications_event", columnNames = {"user_id", "event_key"}), indexes = @Index(name = "idx_user_notifications_unread", columnList = "user_id,read_at,hidden_at,created_at"))
public class UserNotification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(name = "event_key", nullable = false, length = 191) private String eventKey;
    @Column(nullable = false, length = 40) private String category;
    @Column(nullable = false, length = 180) private String title;
    @Column(nullable = false, length = 500) private String body;
    @Column(name = "target_path", length = 2048) private String targetPath;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @Column(name = "read_at") private LocalDateTime readAt;
    @Column(name = "hidden_at") private LocalDateTime hiddenAt;

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getEventKey() { return eventKey; }
    public void setEventKey(String eventKey) { this.eventKey = eventKey; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getTargetPath() { return targetPath; }
    public void setTargetPath(String targetPath) { this.targetPath = targetPath; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
    public LocalDateTime getHiddenAt() { return hiddenAt; }
    public void setHiddenAt(LocalDateTime hiddenAt) { this.hiddenAt = hiddenAt; }
    public boolean isRead() { return readAt != null; }
}
