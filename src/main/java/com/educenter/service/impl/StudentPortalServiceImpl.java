package com.educenter.service.impl;

import com.educenter.dto.StudentHomeworkRow;
import com.educenter.dto.StudentAttendanceRow;
import com.educenter.dto.StudentPortalData;
import com.educenter.entity.*;
import com.educenter.repository.*;
import com.educenter.service.ClassSessionService;
import com.educenter.service.HomeworkService;
import com.educenter.service.StudentPortalService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class StudentPortalServiceImpl implements StudentPortalService {
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final StudentRepository students;
    private final ClassStudentRepository enrollments;
    private final ClassSessionRepository sessions;
    private final ClassSessionService classSessionService;
    private final HomeworkService homework;
    private final GradeRepository grades;
    private final AttendanceRepository attendance;
    private final SessionStudentReportRepository sessionReports;
    private final MonthlyStudentReportRepository monthlyReports;

    public StudentPortalServiceImpl(StudentRepository students, ClassStudentRepository enrollments,
                                    ClassSessionRepository sessions, ClassSessionService classSessionService,
                                    HomeworkService homework, GradeRepository grades, AttendanceRepository attendance,
                                    SessionStudentReportRepository sessionReports, MonthlyStudentReportRepository monthlyReports) {
        this.students = students;
        this.enrollments = enrollments;
        this.sessions = sessions;
        this.classSessionService = classSessionService;
        this.homework = homework;
        this.grades = grades;
        this.attendance = attendance;
        this.sessionReports = sessionReports;
        this.monthlyReports = monthlyReports;
    }

    @Override
    @Transactional
    public StudentPortalData getPortalData(Long studentUserId) {
        Student student = students.findByUserId(studentUserId)
                .filter(s -> Boolean.TRUE.equals(s.getActive()))
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản chưa được liên kết với hồ sơ học sinh đang hoạt động."));
        if (student.getUser() == null || student.getUser().getRole() == null
                || !"ROLE_STUDENT".equals(student.getUser().getRole().getName())
                || !Boolean.TRUE.equals(student.getUser().getActive())) {
            throw new IllegalArgumentException("Tài khoản học sinh không hoạt động.");
        }

        LocalDate today = LocalDate.now(APP_ZONE);
        LocalTime now = LocalTime.now(APP_ZONE);
        List<ClassStudent> active = enrollments.findByStudentId(student.getId()).stream()
                .filter(e -> "ACTIVE".equals(e.getStatus()))
                .filter(e -> e.getEnrolledDate() == null || !e.getEnrolledDate().isAfter(today))
                .filter(e -> e.getDroppedDate() == null || e.getDroppedDate().isAfter(today))
                .toList();
        List<ClassSession> upcoming = new ArrayList<>();
        for (ClassStudent enrollment : active) {
            Long classId = enrollment.getClassEntity().getId();
            classSessionService.synchronizeClass(classId);
            upcoming.addAll(sessions.findUpcomingForClass(classId, today, now).stream()
                    .filter(session -> "SCHEDULED".equals(session.getStatus())).toList());
        }
        upcoming = upcoming.stream().distinct()
                .sorted(Comparator.comparing(ClassSession::getSessionDate).thenComparing(ClassSession::getStartTime))
                .limit(10).toList();

        List<StudentHomeworkRow> homeworkRows = homework.findForStudent(studentUserId);
        List<Attendance> attendanceRows = attendance.findByStudentIdOrderByAttendanceDateDesc(student.getId());
        List<SessionStudentReport> publishedSessionReports = sessionReports.findPublishedForStudent(student.getId());
        List<MonthlyStudentReport> publishedMonthlyReports = monthlyReports
                .findByStudentIdAndPublishedAtIsNotNullOrderByReportMonthDesc(student.getId());
        Set<String> publishedClassDates = new HashSet<>();
        List<StudentAttendanceRow> attendanceSummary = new ArrayList<>();
        for (SessionStudentReport report : publishedSessionReports) {
            ClassSession session = report.getClassSession();
            publishedClassDates.add(attendanceKey(session.getClassEntity().getId(), session.getSessionDate()));
            attendanceSummary.add(new StudentAttendanceRow(session.getClassEntity(), session.getSessionDate(),
                    report.getAttendanceStatus(), report.getNote()));
        }
        for (Attendance row : attendanceRows) {
            if (!publishedClassDates.contains(attendanceKey(row.getClassEntity().getId(), row.getAttendanceDate()))) {
                attendanceSummary.add(new StudentAttendanceRow(row.getClassEntity(), row.getAttendanceDate(), row.getStatus(), row.getNote()));
            }
        }
        attendanceSummary.sort(Comparator.comparing(StudentAttendanceRow::date).reversed());
        return new StudentPortalData(student, active, upcoming, homeworkRows, grades.findByStudentId(student.getId()),
                attendanceSummary, publishedSessionReports, publishedMonthlyReports,
                attendanceSummary.stream().filter(a -> "PRESENT".equals(a.status())).count(),
                attendanceSummary.stream().filter(a -> "ABSENT".equals(a.status())).count(),
                attendanceSummary.stream().filter(a -> "EXCUSED".equals(a.status())).count(),
                homeworkRows.stream().filter(row -> "PENDING".equals(row.status())).count(),
                homeworkRows.stream().filter(row -> "OVERDUE".equals(row.status())).count(),
                homeworkRows.stream().filter(row -> row.submission() != null).count());
    }

    private String attendanceKey(Long classId, LocalDate date) {
        return classId + ":" + date;
    }
}
