package com.educenter.service.impl;

import com.educenter.entity.ClassSession;
import com.educenter.entity.Student;
import com.educenter.entity.Tuition;
import com.educenter.repository.ClassSessionRepository;
import com.educenter.repository.ClassStudentRepository;
import com.educenter.repository.TuitionRepository;
import com.educenter.service.NotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class AcademicReminderDispatcher {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final ClassSessionRepository sessions;
    private final ClassStudentRepository enrollments;
    private final TuitionRepository tuitions;
    private final NotificationService notifications;

    public AcademicReminderDispatcher(ClassSessionRepository sessions, ClassStudentRepository enrollments,
                                      TuitionRepository tuitions, NotificationService notifications) {
        this.sessions = sessions; this.enrollments = enrollments; this.tuitions = tuitions; this.notifications = notifications;
    }

    @Scheduled(fixedDelayString = "${app.notifications.schedule-reminder-interval-ms:60000}")
    @Transactional
    public void scheduleReminders() {
        LocalDateTime now = LocalDateTime.now(ZONE);
        LocalDate today = now.toLocalDate();
        LocalDateTime from = now.plusMinutes(55), to = now.plusMinutes(60);
        for (ClassSession session : sessions.findBySessionDateBetweenOrderBySessionDateAscStartTimeAsc(today, today.plusDays(1))) {
            if (!"SCHEDULED".equals(session.getStatus())) continue;
            LocalDateTime start = LocalDateTime.of(session.getSessionDate(), session.getStartTime());
            if (start.isBefore(from) || start.isAfter(to)) continue;
            String event = "schedule-reminder-" + session.getId() + "-" + session.getSessionDate() + "-" + session.getStartTime();
            String details = session.getClassEntity().getName() + " · " + session.getWeekdayDisplay() + " " + session.getStartTime()
                    + (session.getRoom() == null ? "" : " · Phòng " + session.getRoom().getName());
            notifyParents(session.getClassEntity().getId(), event, "Sắp đến giờ học", details);
        }
    }

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void tuitionReminders() {
        LocalDate today = LocalDate.now(ZONE);
        for (Tuition tuition : tuitions.findByStatusAndDueDateBetween("UNPAID", today, today.plusDays(3))) {
            Student student = tuition.getStudent();
            if (student == null || student.getParent() == null || student.getParent().getUser() == null) continue;
            notifications.notifyUser(student.getParent().getUser().getId(), "tuition-reminder-" + tuition.getId() + "-" + today,
                    "TUITION_REMINDER", "Nhắc hạn học phí", "Khoản học phí của " + student.getFullName() + " đến hạn ngày " + tuition.getDueDate() + ".",
                    "/parent/tuition");
        }
    }

    private void notifyParents(Long classId, String eventKey, String title, String body) {
        notifications.notifyUsers(enrollments.findByClassEntityIdAndStatus(classId, "ACTIVE").stream()
                        .map(e -> e.getStudent().getParent()).filter(java.util.Objects::nonNull).map(p -> p.getUser())
                        .filter(java.util.Objects::nonNull).map(u -> u.getId()).distinct().toList(),
                eventKey, "SCHEDULE_REMINDER", title, body, "/parent/schedule");
    }
}
