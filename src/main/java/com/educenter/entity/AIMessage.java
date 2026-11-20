package com.educenter.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_messages", indexes = @Index(name = "idx_ai_message_conversation_created", columnList = "conversation_id,created_at"))
public class AIMessage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private AIConversation conversation;
    @Column(name = "sender_role", nullable = false, length = 20)
    private String senderRole;
    @Lob @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;
    @Column(length = 100)
    private String model;
    @Column(name = "prompt_tokens")
    private Integer promptTokens;
    @Column(name = "completion_tokens")
    private Integer completionTokens;
    @CreationTimestamp @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public AIConversation getConversation() { return conversation; }
    public void setConversation(AIConversation conversation) { this.conversation = conversation; }
    public String getSenderRole() { return senderRole; }
    public void setSenderRole(String senderRole) { this.senderRole = senderRole; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Integer getPromptTokens() { return promptTokens; }
    public void setPromptTokens(Integer promptTokens) { this.promptTokens = promptTokens; }
    public Integer getCompletionTokens() { return completionTokens; }
    public void setCompletionTokens(Integer completionTokens) { this.completionTokens = completionTokens; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
