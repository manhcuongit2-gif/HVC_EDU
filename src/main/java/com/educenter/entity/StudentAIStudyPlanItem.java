package com.educenter.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_ai_study_plan_items", uniqueConstraints =
        @UniqueConstraint(name = "uk_student_ai_study_plan_item", columnNames = {"plan_id", "item_key"}))
public class StudentAIStudyPlanItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private StudentAIStudyPlan plan;

    @Column(name = "item_key", nullable = false, length = 160)
    private String itemKey;

    @Column(nullable = false)
    private int position;

    @Column(name = "subject_name", nullable = false, length = 100)
    private String subjectName;

    @Column(nullable = false, length = 150)
    private String topic;

    @Column(nullable = false, length = 500)
    private String goal;

    @Column(name = "practice_prompt", nullable = false, columnDefinition = "TEXT")
    private String practicePrompt;

    @Column(name = "material_references", columnDefinition = "LONGTEXT")
    private String materialReferences;

    @Column(name = "progress_status", nullable = false, length = 20)
    private String progressStatus = "SUGGESTED";

    @CreationTimestamp @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public StudentAIStudyPlan getPlan() { return plan; }
    public void setPlan(StudentAIStudyPlan plan) { this.plan = plan; }
    public String getItemKey() { return itemKey; }
    public void setItemKey(String itemKey) { this.itemKey = itemKey; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }
    public String getPracticePrompt() { return practicePrompt; }
    public void setPracticePrompt(String practicePrompt) { this.practicePrompt = practicePrompt; }
    public String getMaterialReferences() { return materialReferences; }
    public void setMaterialReferences(String materialReferences) { this.materialReferences = materialReferences; }
    public String getProgressStatus() { return progressStatus; }
    public void setProgressStatus(String progressStatus) { this.progressStatus = progressStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
