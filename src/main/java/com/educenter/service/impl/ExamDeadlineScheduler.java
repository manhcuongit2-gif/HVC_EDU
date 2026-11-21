package com.educenter.service.impl;

import com.educenter.service.ExamService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExamDeadlineScheduler {
    private final ExamService exams;
    public ExamDeadlineScheduler(ExamService exams) { this.exams = exams; }
    @Scheduled(fixedDelayString = "${app.exam.expiry-scan-ms:30000}")
    public void submitExpiredAttempts() { exams.autoSubmitExpired(); }
    @Scheduled(fixedDelayString = "${app.exam.reminder-scan-ms:3600000}")
    public void remindUpcomingDeadlines() { exams.notifyUpcomingDeadlines(); }
}
