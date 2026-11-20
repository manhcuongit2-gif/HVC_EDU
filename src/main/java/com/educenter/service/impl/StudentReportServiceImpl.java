package com.educenter.service.impl;

import com.educenter.entity.*;
import com.educenter.repository.*;
import com.educenter.service.ClassSessionService;
import com.educenter.service.StudentReportService;
import com.educenter.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;

@Service
public class StudentReportServiceImpl implements StudentReportService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Set<String> ATTENDANCE_STATUSES = Set.of("PRESENT", "ABSENT", "EXCUSED");

    private final ClassSessionService classSessionService;
    private final ClassSessionRepository sessionRepository;
    private final SessionStudentReportRepository sessionReportRepository;
    private final MonthlyStudentReportRepository monthlyReportRepository;
    private final ClassStudentRepository classStudentRepository;
    private final AttendanceRepository attendanceRepository;
    private final ClassRepository classRepository;
    private final UserRepository userRepository;
    private final MonthlyTuitionBillingService billingService;
    private final NotificationService notificationService;

    public StudentReportServiceImpl(ClassSessionService classSessionService,
                                    ClassSessionRepository sessionRepository,
                                    SessionStudentReportRepository sessionReportRepository,
                                    MonthlyStudentReportRepository monthlyReportRepository,
                                    ClassStudentRepository classStudentRepository,
                                    AttendanceRepository attendanceRepository,
                                    ClassRepository classRepository,
                                    UserRepository userRepository,
                                    MonthlyTuitionBillingService billingService,
                                    NotificationService notificationService) {
        this.classSessionService = classSessionService;
        this.sessionRepository = sessionRepository;
        this.sessionReportRepository = sessionReportRepository;
        this.monthlyReportRepository = monthlyReportRepository;
        this.classStudentRepository = classStudentRepository;
        this.attendanceRepository = attendanceRepository;
        this.classRepository = classRepository;
        this.userRepository = userRepository;
        this.billingService = billingService;
        this.notificationService = notificationService;
    }

    @Override
    public ClassSession getTeacherSession(Long sessionId, Long teacherUserId) {
        ClassSession session = classSessionService.getById(sessionId);
        requireTeacherOwns(session.getClassEntity(), teacherUserId);
        return session;
    }

    @Override
    @Transactional
    public List<SessionStudentReport> getTeacherSessionReports(Long sessionId, Long teacherUserId) {
        ClassSession session = getTeacherSession(sessionId, teacherUserId);
        if ("CANCELLED".equals(session.getStatus())) throw new IllegalArgumentException("Buổi học đã bị hủy.");
        return ensureReportRows(session);
    }

    @Override
    public List<SessionStudentReport> getAdminSessionReports(Long sessionId) {
        return sessionReportRepository.findByClassSessionIdOrderByStudentFullNameAsc(sessionId);
    }

    @Override
    @Transactional
    public void saveSessionReports(Long sessionId, Long teacherUserId, List<Long> studentIds,
                                   List<String> attendanceStatuses, List<String> homeworkScores,
                                   List<String> comments, List<String> goodEvaluationCounts, List<String> notes) {
        ClassSession session = getTeacherSession(sessionId, teacherUserId);
        ensureSessionEnded(session);
        if ("CANCELLED".equals(session.getStatus())) throw new IllegalArgumentException("Buổi học đã bị hủy.");

        List<SessionStudentReport> reports = ensureReportRows(session);
        if (reports.isEmpty()) throw new IllegalArgumentException("Lớp chưa có học sinh được ghi danh tại ngày học này.");
        validateListLengths(reports.size(), studentIds, attendanceStatuses, homeworkScores, comments, goodEvaluationCounts, notes);
        Map<Long, SessionStudentReport> byStudent = new HashMap<>();
        for (SessionStudentReport report : reports) byStudent.put(report.getStudent().getId(), report);
        User teacher = userRepository.findById(teacherUserId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản người dạy."));

        for (int i = 0; i < studentIds.size(); i++) {
            SessionStudentReport report = byStudent.get(studentIds.get(i));
            if (report == null) throw new IllegalArgumentException("Danh sách học sinh báo cáo không hợp lệ.");
            String attendance = valueAt(attendanceStatuses, i);
            String comment = valueAt(comments, i);
            String scoreValue = valueAt(homeworkScores, i);
            String countValue = valueAt(goodEvaluationCounts, i);
            String note = valueAt(notes, i);
            if (!ATTENDANCE_STATUSES.contains(attendance)) throw new IllegalArgumentException("Trạng thái điểm danh không hợp lệ.");
            if (comment.isBlank()) throw new IllegalArgumentException("Vui lòng nhập nhận xét cho từng học sinh.");

            BigDecimal score = parseOptionalDecimal(scoreValue, "Điểm BTVN");
            if (score != null && (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(BigDecimal.TEN) > 0)) {
                throw new IllegalArgumentException("Điểm BTVN phải từ 0 đến 10.");
            }
            int goodCount = parseOptionalInteger(countValue, 0, "Số lượt đánh giá tốt");
            if (goodCount < 0) throw new IllegalArgumentException("Số lượt đánh giá tốt không được âm.");

            report.setAttendanceStatus(attendance);
            report.setHomeworkScore(score);
            report.setComment(comment.trim());
            report.setGoodEvaluationCount(goodCount);
            report.setNote(note.isBlank() ? null : note.trim());
            report.setUpdatedBy(teacher);
            if (report.getCreatedBy() == null) report.setCreatedBy(teacher);
        }
        sessionReportRepository.saveAll(reports);
        refreshPublishedMonthForSession(session);
    }

    @Override
    @Transactional
    public void publishSession(Long sessionId, Long teacherUserId) {
        ClassSession session = getTeacherSession(sessionId, teacherUserId);
        ensureSessionEnded(session);
        if ("CANCELLED".equals(session.getStatus())) throw new IllegalArgumentException("Buổi học đã bị hủy.");
        List<SessionStudentReport> reports = ensureReportRows(session);
        validateCompleteReports(reports);

        if (session.getPublishedAt() == null) {
            session.setPublishedAt(LocalDateTime.now(APP_ZONE));
            session.setPublishedBy(userRepository.findById(teacherUserId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản người dạy.")));
            sessionRepository.save(session);
        }
        refreshPublishedMonthForSession(session);
        for (SessionStudentReport report : reports) {
            if (report.getStudent().getParent() != null && report.getStudent().getParent().getUser() != null) {
                notificationService.notifyUser(report.getStudent().getParent().getUser().getId(),
                        "learning-report-session-" + sessionId + "-" + report.getStudent().getId(), "LEARNING_REPORT",
                        "Báo cáo buổi học mới", "Đã có nhận xét buổi " + session.getSessionDate() + " của " + report.getStudent().getFullName() + ".",
                        "/parent/reports");
            }
        }
    }

    @Override
    @Transactional
    public List<MonthlyStudentReport> getMonthlyReports(Long classId, YearMonth month, Long teacherUserId) {
        ClassEntity classEntity = getTeacherClass(classId, teacherUserId);
        List<ClassSession> sessions = sessionRepository.findReportableForClassBetween(classId, month.atDay(1), month.atEndOfMonth());
        for (ClassSession session : sessions) {
            if (hasEnded(session)) ensureReportRows(session);
        }

        List<SessionStudentReport> rows = sessionReportRepository.findForClassAndMonth(classId, month.atDay(1), month.atEndOfMonth());
        Map<Long, Student> students = new TreeMap<>();
        rows.forEach(row -> students.put(row.getStudent().getId(), row.getStudent()));
        List<MonthlyStudentReport> result = new ArrayList<>();
        for (Student student : students.values()) {
            MonthlyStudentReport report = monthlyReportRepository.findByClassEntityIdAndStudentIdAndReportMonth(classId, student.getId(), month.atDay(1))
                    .orElseGet(() -> {
                        MonthlyStudentReport created = new MonthlyStudentReport();
                        created.setClassEntity(classEntity);
                        created.setStudent(student);
                        created.setReportMonth(month.atDay(1));
                        created.setTeacherComment("");
                        return created;
                    });
            recalculateMonthlySummary(report, month);
            result.add(monthlyReportRepository.save(report));
        }
        return result;
    }

    @Override
    @Transactional
    public boolean publishMonthlyReport(Long classId, Long studentId, YearMonth month, String comment, Long teacherUserId) {
        ClassEntity classEntity = getTeacherClass(classId, teacherUserId);
        if (comment == null || comment.isBlank()) throw new IllegalArgumentException("Vui lòng nhập nhận xét tổng kết tháng.");
        List<ClassSession> dueSessions = sessionRepository.findReportableForClassBetween(classId, month.atDay(1), min(month.atEndOfMonth(), LocalDate.now(APP_ZONE)))
                .stream().filter(this::hasEnded).toList();
        List<ClassSession> missing = dueSessions.stream().filter(s -> s.getPublishedAt() == null).toList();
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Còn " + missing.size() + " buổi đã kết thúc chưa được người dạy công bố báo cáo.");
        }

        List<SessionStudentReport> studentRows = sessionReportRepository.findPublishedForStudentAndMonth(
                classId, studentId, month.atDay(1), month.atEndOfMonth());
        if (studentRows.isEmpty()) throw new IllegalArgumentException("Học sinh chưa có báo cáo buổi nào trong tháng này.");
        Student student = studentRows.get(0).getStudent();
        MonthlyStudentReport report = monthlyReportRepository.findByClassEntityIdAndStudentIdAndReportMonth(classId, studentId, month.atDay(1))
                .orElseGet(MonthlyStudentReport::new);
        report.setClassEntity(classEntity);
        report.setStudent(student);
        report.setReportMonth(month.atDay(1));
        report.setTeacherComment(comment.trim());
        report.setPublishedAt(LocalDateTime.now(APP_ZONE));
        report.setPublishedBy(userRepository.findById(teacherUserId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản người dạy.")));
        report.setUpdatedBy(report.getPublishedBy());
        recalculateMonthlySummary(report, month);
        monthlyReportRepository.save(report);
        if (student.getParent() != null && student.getParent().getUser() != null) {
            notificationService.notifyUser(student.getParent().getUser().getId(),
                    "learning-report-monthly-" + classId + "-" + studentId + "-" + month, "LEARNING_REPORT",
                    "Báo cáo học tập tháng mới", "Đã có tổng kết tháng " + month.getMonthValue() + "/" + month.getYear() + " cho " + student.getFullName() + ".",
                    "/parent/reports");
        }
        BigDecimal calculatedTotal = classEntity.getSessionFee() == null ? BigDecimal.ZERO
                : classEntity.getSessionFee().multiply(BigDecimal.valueOf(report.getPresentCount()));
        return billingService.synchronizeCharge(student, classEntity, month, calculatedTotal);
    }

    @Override
    public List<SessionStudentReport> getPublishedSessionReportsForParent(Long parentUserId) {
        return sessionReportRepository.findPublishedForParent(parentUserId);
    }

    @Override
    public List<MonthlyStudentReport> getPublishedMonthlyReportsForParent(Long parentUserId) {
        return monthlyReportRepository.findPublishedForParent(parentUserId);
    }

    private List<SessionStudentReport> ensureReportRows(ClassSession session) {
        List<ClassStudent> enrollments = classStudentRepository.findByClassEntityIdOrderByStudentFullNameAsc(session.getClassEntity().getId());
        Map<Long, SessionStudentReport> existing = new HashMap<>();
        sessionReportRepository.findByClassSessionId(session.getId()).forEach(row -> existing.put(row.getStudent().getId(), row));
        List<SessionStudentReport> reports = new ArrayList<>();
        for (ClassStudent enrollment : enrollments) {
            LocalDate date = session.getSessionDate();
            if (enrollment.getEnrolledDate() != null && enrollment.getEnrolledDate().isAfter(date)) continue;
            if (enrollment.getDroppedDate() != null && !date.isBefore(enrollment.getDroppedDate())) continue;
            SessionStudentReport report = existing.get(enrollment.getStudent().getId());
            if (report == null) {
                report = new SessionStudentReport();
                report.setClassSession(session);
                report.setStudent(enrollment.getStudent());
                report.setAttendanceStatus("PRESENT");
                report.setGoodEvaluationCount(0);
                Optional<Attendance> legacyAttendance = attendanceRepository.findByClassEntityIdAndStudentIdAndAttendanceDate(
                        session.getClassEntity().getId(), enrollment.getStudent().getId(), session.getSessionDate());
                if (legacyAttendance.isPresent()) {
                    report.setAttendanceStatus(legacyAttendance.get().getStatus());
                    report.setNote(legacyAttendance.get().getNote());
                }
                report.setComment("");
                existing.put(enrollment.getStudent().getId(), report);
            }
            reports.add(report);
        }
        return sessionReportRepository.saveAll(reports);
    }

    private void validateCompleteReports(List<SessionStudentReport> reports) {
        if (reports.isEmpty()) throw new IllegalArgumentException("Lớp chưa có học sinh trong buổi học này.");
        for (SessionStudentReport report : reports) {
            if (!ATTENDANCE_STATUSES.contains(report.getAttendanceStatus())) throw new IllegalArgumentException("Thiếu hoặc sai trạng thái điểm danh.");
            if (report.getComment() == null || report.getComment().isBlank()) throw new IllegalArgumentException("Còn học sinh chưa có nhận xét.");
            if (report.getHomeworkScore() != null && (report.getHomeworkScore().signum() < 0 || report.getHomeworkScore().compareTo(BigDecimal.TEN) > 0)) {
                throw new IllegalArgumentException("Điểm BTVN phải từ 0 đến 10.");
            }
            if (report.getGoodEvaluationCount() == null || report.getGoodEvaluationCount() < 0) {
                throw new IllegalArgumentException("Số lượt đánh giá tốt không hợp lệ.");
            }
        }
    }

    private void refreshPublishedMonthForSession(ClassSession session) {
        YearMonth month = YearMonth.from(session.getSessionDate());
        for (SessionStudentReport row : sessionReportRepository.findByClassSessionId(session.getId())) {
            monthlyReportRepository.findByClassEntityIdAndStudentIdAndReportMonth(
                    session.getClassEntity().getId(), row.getStudent().getId(), month.atDay(1))
                    .filter(report -> report.getPublishedAt() != null)
                    .ifPresent(report -> {
                        recalculateMonthlySummary(report, month);
                        monthlyReportRepository.save(report);
                        if (session.getClassEntity().getSessionFee() != null) {
                            billingService.synchronizeCharge(row.getStudent(), session.getClassEntity(), month,
                                    session.getClassEntity().getSessionFee().multiply(BigDecimal.valueOf(report.getPresentCount())));
                        }
                    });
        }
    }

    private void recalculateMonthlySummary(MonthlyStudentReport report, YearMonth month) {
        List<SessionStudentReport> rows = sessionReportRepository.findPublishedForStudentAndMonth(
                report.getClassEntity().getId(), report.getStudent().getId(), month.atDay(1), month.atEndOfMonth());
        int present = 0, absent = 0, excused = 0, good = 0, scoreCount = 0;
        BigDecimal scoreTotal = BigDecimal.ZERO;
        for (SessionStudentReport row : rows) {
            switch (row.getAttendanceStatus()) {
                case "PRESENT" -> present++;
                case "ABSENT" -> absent++;
                case "EXCUSED" -> excused++;
            }
            good += row.getGoodEvaluationCount() == null ? 0 : row.getGoodEvaluationCount();
            if (row.getHomeworkScore() != null) {
                scoreTotal = scoreTotal.add(row.getHomeworkScore());
                scoreCount++;
            }
        }
        report.setPresentCount(present);
        report.setAbsentCount(absent);
        report.setExcusedCount(excused);
        report.setGoodEvaluationCount(good);
        report.setHomeworkAverage(scoreCount == 0 ? null : scoreTotal.divide(BigDecimal.valueOf(scoreCount), 2, RoundingMode.HALF_UP));
    }

    private ClassEntity getTeacherClass(Long classId, Long teacherUserId) {
        ClassEntity classEntity = classRepository.findById(classId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lớp học."));
        requireTeacherOwns(classEntity, teacherUserId);
        return classEntity;
    }

    private void requireTeacherOwns(ClassEntity classEntity, Long teacherUserId) {
        if (classEntity.getTeacher() == null || classEntity.getTeacher().getUser() == null
                || !Objects.equals(classEntity.getTeacher().getUser().getId(), teacherUserId)) {
            throw new IllegalArgumentException("Bạn không được phân công phụ trách lớp học này.");
        }
    }

    private void ensureSessionEnded(ClassSession session) {
        if (!hasEnded(session)) throw new IllegalArgumentException("Chỉ có thể nhập báo cáo sau khi buổi học kết thúc.");
    }

    private boolean hasEnded(ClassSession session) {
        return !LocalDateTime.of(session.getSessionDate(), session.getEndTime()).isAfter(LocalDateTime.now(APP_ZONE));
    }

    private void validateListLengths(int expected, List<?>... lists) {
        for (List<?> list : lists) {
            if (list == null || list.size() != expected) throw new IllegalArgumentException("Dữ liệu báo cáo chưa đầy đủ cho toàn bộ học sinh.");
        }
    }

    private String valueAt(List<String> values, int index) {
        return values.get(index) == null ? "" : values.get(index).trim();
    }

    private BigDecimal parseOptionalDecimal(String raw, String label) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " không hợp lệ.");
        }
    }

    private int parseOptionalInteger(String raw, int defaultValue, String label) {
        if (raw == null || raw.isBlank()) return defaultValue;
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " phải là số nguyên không âm.");
        }
    }

    private LocalDate min(LocalDate left, LocalDate right) {
        return left.isBefore(right) ? left : right;
    }
}
