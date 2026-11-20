package com.educenter.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_deliveries", uniqueConstraints = @UniqueConstraint(name = "uk_notification_delivery_device", columnNames = {"outbox_id", "subscription_id"}))
public class NotificationDelivery {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "outbox_id", nullable = false) private NotificationOutbox outbox;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "subscription_id", nullable = false) private PushSubscription subscription;
    @Column(nullable = false, length = 20) private String status = "PENDING";
    @Column(name = "attempt_count", nullable = false) private int attemptCount;
    @Column(name = "accepted_at") private LocalDateTime acceptedAt;
    @Column(name = "last_error", length = 500) private String lastError;

    public Long getId() { return id; }
    public NotificationOutbox getOutbox() { return outbox; }
    public void setOutbox(NotificationOutbox outbox) { this.outbox = outbox; }
    public PushSubscription getSubscription() { return subscription; }
    public void setSubscription(PushSubscription subscription) { this.subscription = subscription; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }
    public LocalDateTime getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(LocalDateTime acceptedAt) { this.acceptedAt = acceptedAt; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
}
