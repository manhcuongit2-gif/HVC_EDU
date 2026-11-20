package com.educenter.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_usage", indexes = {
        @Index(name = "idx_ai_usage_created_status", columnList = "created_at,status"),
        @Index(name = "idx_ai_usage_user_created", columnList = "user_id,created_at")
})
public class AIUsage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
    private AIConversation conversation;
    @Column(name = "role_name", nullable = false, length = 30)
    private String roleName;
    @Column(nullable = false, length = 100)
    private String model;
    @Column(nullable = false, length = 20)
    private String status = "PENDING";
    @Column(name = "provider_status")
    private Integer providerStatus;
    @Column(name = "prompt_tokens", nullable = false)
    private long promptTokens;
    @Column(name = "completion_tokens", nullable = false)
    private long completionTokens;
    @Column(name = "reserved_tokens", nullable = false)
    private long reservedTokens;
    @Column(name = "provider_remaining_requests")
    private Long providerRemainingRequests;
    @Column(name = "provider_remaining_tokens")
    private Long providerRemainingTokens;
    @Column(name = "error_code", length = 50)
    private String errorCode;
    @CreationTimestamp @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public AIConversation getConversation() { return conversation; }
    public void setConversation(AIConversation conversation) { this.conversation = conversation; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getProviderStatus() { return providerStatus; }
    public void setProviderStatus(Integer providerStatus) { this.providerStatus = providerStatus; }
    public long getPromptTokens() { return promptTokens; }
    public void setPromptTokens(long promptTokens) { this.promptTokens = promptTokens; }
    public long getCompletionTokens() { return completionTokens; }
    public void setCompletionTokens(long completionTokens) { this.completionTokens = completionTokens; }
    public long getReservedTokens() { return reservedTokens; }
    public void setReservedTokens(long reservedTokens) { this.reservedTokens = reservedTokens; }
    public Long getProviderRemainingRequests() { return providerRemainingRequests; }
    public void setProviderRemainingRequests(Long providerRemainingRequests) { this.providerRemainingRequests = providerRemainingRequests; }
    public Long getProviderRemainingTokens() { return providerRemainingTokens; }
    public void setProviderRemainingTokens(Long providerRemainingTokens) { this.providerRemainingTokens = providerRemainingTokens; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
