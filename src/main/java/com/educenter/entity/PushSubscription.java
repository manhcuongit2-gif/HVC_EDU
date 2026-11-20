package com.educenter.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "push_subscriptions", uniqueConstraints = @UniqueConstraint(name = "uk_push_endpoint_hash", columnNames = "endpoint_hash"))
public class PushSubscription {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Lob @Column(nullable = false)
    private String endpoint;

    @Column(name = "endpoint_hash", nullable = false, length = 64)
    private String endpointHash;

    @Column(name = "p256dh_key", nullable = false, length = 256)
    private String p256dhKey;

    @Column(name = "auth_key", nullable = false, length = 128)
    private String authKey;

    @Column(name = "device_name", nullable = false, length = 100)
    private String deviceName = "Thiết bị của tôi";

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(length = 80)
    private String platform;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getEndpointHash() { return endpointHash; }
    public void setEndpointHash(String endpointHash) { this.endpointHash = endpointHash; }
    public String getP256dhKey() { return p256dhKey; }
    public void setP256dhKey(String p256dhKey) { this.p256dhKey = p256dhKey; }
    public String getAuthKey() { return authKey; }
    public void setAuthKey(String authKey) { this.authKey = authKey; }
    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastSeenAt() { return lastSeenAt; }
}
