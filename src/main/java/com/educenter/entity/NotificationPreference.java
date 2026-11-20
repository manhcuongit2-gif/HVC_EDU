package com.educenter.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "notification_preferences", uniqueConstraints = @UniqueConstraint(name = "uk_notification_preferences_user", columnNames = "user_id"))
public class NotificationPreference {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "push_enabled", nullable = false) private boolean pushEnabled = true;
    @Column(name = "private_messages", nullable = false) private boolean privateMessages = true;
    @Column(name = "group_messages", nullable = false) private boolean groupMessages = true;
    @Column(nullable = false) private boolean tuition = true;
    @Column(name = "tuition_reminders", nullable = false) private boolean tuitionReminders = true;
    @Column(name = "schedule_reminders", nullable = false) private boolean scheduleReminders = true;
    @Column(name = "schedule_changes", nullable = false) private boolean scheduleChanges = true;
    @Column(nullable = false) private boolean attendance = true;
    @Column(name = "learning_reports", nullable = false) private boolean learningReports = true;
    @Column(nullable = false) private boolean homework = true;
    @Column(nullable = false) private boolean general = true;
    @Column(name = "sound_enabled", nullable = false) private boolean soundEnabled = true;
    @Column(name = "preview_enabled", nullable = false) private boolean previewEnabled;
    @Column(name = "quiet_hours_enabled", nullable = false) private boolean quietHoursEnabled;
    @Column(name = "quiet_start") private LocalTime quietStart;
    @Column(name = "quiet_end") private LocalTime quietEnd;
    @UpdateTimestamp @Column(name = "updated_at") private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public boolean isPushEnabled() { return pushEnabled; }
    public void setPushEnabled(boolean pushEnabled) { this.pushEnabled = pushEnabled; }
    public boolean isPrivateMessages() { return privateMessages; }
    public void setPrivateMessages(boolean privateMessages) { this.privateMessages = privateMessages; }
    public boolean isGroupMessages() { return groupMessages; }
    public void setGroupMessages(boolean groupMessages) { this.groupMessages = groupMessages; }
    public boolean isTuition() { return tuition; }
    public void setTuition(boolean tuition) { this.tuition = tuition; }
    public boolean isTuitionReminders() { return tuitionReminders; }
    public void setTuitionReminders(boolean tuitionReminders) { this.tuitionReminders = tuitionReminders; }
    public boolean isScheduleReminders() { return scheduleReminders; }
    public void setScheduleReminders(boolean scheduleReminders) { this.scheduleReminders = scheduleReminders; }
    public boolean isScheduleChanges() { return scheduleChanges; }
    public void setScheduleChanges(boolean scheduleChanges) { this.scheduleChanges = scheduleChanges; }
    public boolean isAttendance() { return attendance; }
    public void setAttendance(boolean attendance) { this.attendance = attendance; }
    public boolean isLearningReports() { return learningReports; }
    public void setLearningReports(boolean learningReports) { this.learningReports = learningReports; }
    public boolean isHomework() { return homework; }
    public void setHomework(boolean homework) { this.homework = homework; }
    public boolean isGeneral() { return general; }
    public void setGeneral(boolean general) { this.general = general; }
    public boolean isSoundEnabled() { return soundEnabled; }
    public void setSoundEnabled(boolean soundEnabled) { this.soundEnabled = soundEnabled; }
    public boolean isPreviewEnabled() { return previewEnabled; }
    public void setPreviewEnabled(boolean previewEnabled) { this.previewEnabled = previewEnabled; }
    public boolean isQuietHoursEnabled() { return quietHoursEnabled; }
    public void setQuietHoursEnabled(boolean quietHoursEnabled) { this.quietHoursEnabled = quietHoursEnabled; }
    public LocalTime getQuietStart() { return quietStart; }
    public void setQuietStart(LocalTime quietStart) { this.quietStart = quietStart; }
    public LocalTime getQuietEnd() { return quietEnd; }
    public void setQuietEnd(LocalTime quietEnd) { this.quietEnd = quietEnd; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
