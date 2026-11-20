package com.educenter.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_outbox", uniqueConstraints = @UniqueConstraint(name = "uk_notification_outbox_event", columnNames = "event_key"), indexes = @Index(name = "idx_notification_outbox_pending", columnList = "status,scheduled_at,next_attempt_at"))
public class NotificationOutbox {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "notification_id") private UserNotification notification;
    @Column(name = "event_key", nullable = false, length = 191) private String eventKey;
    @Column(nullable = false, length = 40) private String category;
    @Column(nullable = false, length = 180) private String title;
    @Column(nullable = false, length = 500) private String body;
    @Column(name = "target_path", length = 2048) private String targetPath;
    @Column(name = "scheduled_at", nullable = false) private LocalDateTime scheduledAt;
    @Column(nullable = false, length = 20) private String status = "PENDING";
    @Column(name = "attempt_count", nullable = false) private int attemptCount;
    @Column(name = "next_attempt_at") private LocalDateTime nextAttemptAt;
    @Column(name = "last_error", length = 500) private String lastError;
    @Column(name = "accepted_at") private LocalDateTime acceptedAt;
    @Column(name = "sound_enabled", nullable = false) private boolean soundEnabled = true;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public UserNotification getNotification() { return notification; }
    public void setNotification(UserNotification notification) { this.notification = notification; }
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
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }
    public LocalDateTime getNextAttemptAt() { return nextAttemptAt; }
    public void setNextAttemptAt(LocalDateTime nextAttemptAt) { this.nextAttemptAt = nextAttemptAt; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public LocalDateTime getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(LocalDateTime acceptedAt) { this.acceptedAt = acceptedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public boolean isSoundEnabled() { return soundEnabled; }
    public void setSoundEnabled(boolean soundEnabled) { this.soundEnabled = soundEnabled; }
}
