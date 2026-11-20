package com.educenter.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_settings")
public class AISettings {
    @Id
    private Integer id = 1;
    @Column(nullable = false)
    private boolean enabled;
    @Column(name = "admin_enabled", nullable = false)
    private boolean adminEnabled;
    @Column(name = "teacher_enabled", nullable = false)
    private boolean teacherEnabled;
    @Column(name = "parent_enabled", nullable = false)
    private boolean parentEnabled;
    @Column(name = "per_user_daily_requests")
    private Integer perUserDailyRequests;
    @Column(name = "global_daily_requests")
    private Integer globalDailyRequests;
    @Column(name = "daily_token_limit")
    private Long dailyTokenLimit;
    @Column(name = "max_prompt_characters")
    private Integer maxPromptCharacters;
    @Column(name = "max_response_tokens")
    private Integer maxResponseTokens;
    @Column(name = "max_concurrent_requests")
    private Integer maxConcurrentRequests;
    @Column(name = "allowed_models", nullable = false, length = 500)
    private String allowedModels = "openai/gpt-oss-120b";
    @Column(name = "selected_model", nullable = false, length = 100)
    private String selectedModel = "openai/gpt-oss-120b";
    @UpdateTimestamp @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Integer getId() { return id; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isAdminEnabled() { return adminEnabled; }
    public void setAdminEnabled(boolean adminEnabled) { this.adminEnabled = adminEnabled; }
    public boolean isTeacherEnabled() { return teacherEnabled; }
    public void setTeacherEnabled(boolean teacherEnabled) { this.teacherEnabled = teacherEnabled; }
    public boolean isParentEnabled() { return parentEnabled; }
    public void setParentEnabled(boolean parentEnabled) { this.parentEnabled = parentEnabled; }
    public Integer getPerUserDailyRequests() { return perUserDailyRequests; }
    public void setPerUserDailyRequests(Integer value) { this.perUserDailyRequests = value; }
    public Integer getGlobalDailyRequests() { return globalDailyRequests; }
    public void setGlobalDailyRequests(Integer value) { this.globalDailyRequests = value; }
    public Long getDailyTokenLimit() { return dailyTokenLimit; }
    public void setDailyTokenLimit(Long value) { this.dailyTokenLimit = value; }
    public Integer getMaxPromptCharacters() { return maxPromptCharacters; }
    public void setMaxPromptCharacters(Integer value) { this.maxPromptCharacters = value; }
    public Integer getMaxResponseTokens() { return maxResponseTokens; }
    public void setMaxResponseTokens(Integer value) { this.maxResponseTokens = value; }
    public Integer getMaxConcurrentRequests() { return maxConcurrentRequests; }
    public void setMaxConcurrentRequests(Integer value) { this.maxConcurrentRequests = value; }
    public String getAllowedModels() { return allowedModels; }
    public void setAllowedModels(String allowedModels) { this.allowedModels = allowedModels; }
    public String getSelectedModel() { return selectedModel; }
    public void setSelectedModel(String selectedModel) { this.selectedModel = selectedModel; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
