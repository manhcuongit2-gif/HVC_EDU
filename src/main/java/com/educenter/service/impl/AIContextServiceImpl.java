package com.educenter.service.impl;

import com.educenter.entity.*;
import com.educenter.repository.*;
import com.educenter.service.AIContextService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AIContextServiceImpl implements AIContextService {
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Set<String> TOPICS = Set.of("ADMIN_OVERVIEW", "ADMIN_FINANCE", "ADMIN_CLASS_SIZES",
            "ADMIN_REPORT_STATUS", "ADMIN_NEW_STUDENTS", "STUDENT_PROGRESS", "LEARNING_SUMMARY",
            "SCHEDULE", "TUITION", "HOMEWORK");
    private final UserRepository userRepository;
    private final ParentRepository parentRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final ClassRepository classRepository;
    private final ClassStudentRepository classStudentRepository;
    private final ClassSessionRepository classSessionRepository;
    private final GradeRepository gradeRepository;
    private final AttendanceRepository attendanceRepository;
    private final TuitionRepository tuitionRepository;
    private final PaymentRepository paymentRepository;
    private final RegistrationRepository registrationRepository;
    private final SessionStudentReportRepository reportRepository;
    private final HomeworkAssignmentRepository homeworkAssignmentRepository;
    private final HomeworkSubmissionRepository homeworkSubmissionRepository;
    private final ObjectMapper objectMapper;

    public AIContextServiceImpl(UserRepository userRepository, ParentRepository parentRepository,
                                StudentRepository studentRepository, TeacherRepository teacherRepository,
                                ClassRepository classRepository, ClassStudentRepository classStudentRepository,
                                ClassSessionRepository classSessionRepository, GradeRepository gradeRepository,
                                AttendanceRepository attendanceRepository, TuitionRepository tuitionRepository,
                                PaymentRepository paymentRepository, RegistrationRepository registrationRepository,
                                SessionStudentReportRepository reportRepository,
                                HomeworkAssignmentRepository homeworkAssignmentRepository,
                                HomeworkSubmissionRepository homeworkSubmissionRepository,
                                ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.parentRepository = parentRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.classRepository = classRepository;
        this.classStudentRepository = classStudentRepository;
        this.classSessionRepository = classSessionRepository;
        this.gradeRepository = gradeRepository;
        this.attendanceRepository = attendanceRepository;
        this.tuitionRepository = tuitionRepository;
        this.paymentRepository = paymentRepository;
        this.registrationRepository = registrationRepository;
        this.reportRepository = reportRepository;
        this.homeworkAssignmentRepository = homeworkAssignmentRepository;
        this.homeworkSubmissionRepository = homeworkSubmissionRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<Map<String, Object>> toolDefinitions(String role) {
        List<String> topics = switch (role) {
            case "ROLE_ADMIN" -> List.of("ADMIN_OVERVIEW", "ADMIN_FINANCE", "ADMIN_CLASS_SIZES",
                    "ADMIN_REPORT_STATUS", "ADMIN_NEW_STUDENTS", "STUDENT_PROGRESS");
            case "ROLE_TEACHER" -> List.of("LEARNING_SUMMARY", "STUDENT_PROGRESS", "SCHEDULE", "HOMEWORK");
            case "ROLE_PARENT" -> List.of("LEARNING_SUMMARY", "SCHEDULE", "TUITION", "HOMEWORK");
            default -> List.of();
        };
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("topic", Map.of("type", "string", "enum", topics,
                "description", "Loại dữ liệu cần tra cứu trong danh sách cho phép."));
        properties.put("period", Map.of("type", "string", "description", "Tháng cần xem theo YYYY-MM; bỏ trống để dùng tháng hiện tại."));
        Map<String, Object> parameters = Map.of("type", "object", "properties", properties, "required", List.of("topic"), "additionalProperties", false);
        Map<String, Object> function = Map.of("name", "query_education_data",
                "description", "Tra cứu dữ liệu học tập/quản lý đã được hệ thống phân quyền. Chỉ dùng khi câu trả lời cần dữ liệu thật.",
                "parameters", parameters);
        return List.of(Map.of("type", "function", "function", function));
    }

    @Override
    public AIContextResult execute(Long userId, String role, Long contextId, String topic, String period) {
        if (!TOPICS.contains(topic)) throw new IllegalArgumentException("Loại dữ liệu AI yêu cầu không được hỗ trợ.");
        YearMonth month = parseMonth(period);
        Map<String, String> replacements = new LinkedHashMap<>();
        Map<String, Object> data = switch (role) {
            case "ROLE_ADMIN" -> adminData(topic, month, replacements);
            case "ROLE_TEACHER" -> teacherData(userId, contextId, topic, month, replacements);
            case "ROLE_PARENT" -> parentData(userId, contextId, topic, month);
            default -> throw new IllegalArgumentException("Vai trò không có quyền tra cứu dữ liệu AI.");
        };
        try {
            return new AIContextResult(objectMapper.writeValueAsString(data), replacements);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không thể chuẩn bị dữ liệu AI.");
        }
    }

    @Override
    public String localAnswer(String role, String topic, String jsonData) {
        if (!"ROLE_PARENT".equals(role) || !Set.of("LEARNING_SUMMARY", "STUDENT_PROGRESS", "SCHEDULE", "TUITION", "HOMEWORK").contains(topic))
            return null;
        try {
            var data = objectMapper.readTree(jsonData);
            if (data.has("error")) return "Không thể truy cập dữ liệu cho học sinh đã chọn. Vui lòng kiểm tra phạm vi hoặc thử lại.";
            return switch (topic) {
                case "LEARNING_SUMMARY", "STUDENT_PROGRESS" -> formatParentLearning(data);
                case "SCHEDULE" -> formatParentSchedule(data);
                case "TUITION" -> formatParentTuition(data);
                case "HOMEWORK" -> formatParentHomework(data);
                default -> null;
            };
        } catch (Exception e) {
            return "Đã truy vấn dữ liệu trong hệ thống nhưng không thể định dạng kết quả. Vui lòng thử lại.";
        }
    }

    private String formatParentLearning(com.fasterxml.jackson.databind.JsonNode data) {
        StringBuilder answer = new StringBuilder("Tình hình học tập tháng ")
                .append(data.path("month").asText("hiện tại")).append(":\n")
                .append("- Điểm trung bình các môn: ").append(data.path("overallGradeAverage").asText("Chưa đủ dữ liệu")).append("\n")
                .append("- Điểm bài tập trung bình: ").append(data.path("homeworkAverage").asText("Chưa có điểm")).append("\n")
                .append("- Buổi có mặt: ").append(data.path("publishedPresent").asText("0")).append("\n")
                .append("- Buổi vắng: ").append(data.path("publishedAbsent").asText("0"));
        var subjects = data.path("gradeAverages");
        if (subjects.isArray() && !subjects.isEmpty()) {
            answer.append("\n\nĐiểm theo môn:");
            subjects.forEach(item -> answer.append("\n- ").append(item.path("subject").asText())
                    .append(": ").append(item.path("average").asText("Chưa đủ dữ liệu")));
        }
        var comments = data.path("teacherComments");
        if (comments.isArray() && !comments.isEmpty()) {
            answer.append("\n\nNhận xét đã công bố:");
            comments.forEach(comment -> answer.append("\n- ").append(comment.asText()));
        }
        return answer.toString();
    }

    private String formatParentSchedule(com.fasterxml.jackson.databind.JsonNode data) {
        var sessions = data.path("upcomingSessions");
        if (!sessions.isArray() || sessions.isEmpty()) return "Hiện chưa có buổi học sắp tới trong lịch của con.";
        StringBuilder answer = new StringBuilder("Các buổi học sắp tới:");
        sessions.forEach(item -> answer.append("\n- ").append(item.path("class").asText("Lớp học"))
                .append(" · ").append(item.path("date").asText()).append(" · ")
                .append(item.path("start").asText()).append("–").append(item.path("end").asText()));
        return answer.toString();
    }

    private String formatParentTuition(com.fasterxml.jackson.databind.JsonNode data) {
        StringBuilder answer = new StringBuilder("Học phí tháng ").append(data.path("month").asText("hiện tại"))
                .append(":\n- Đã thanh toán: ").append(formatMoney(data.path("paid")))
                .append("\n- Còn phải thanh toán: ").append(formatMoney(data.path("unpaidOrProcessing")));
        var items = data.path("items");
        if (items.isArray() && !items.isEmpty()) {
            answer.append("\n\nCác khoản trong tháng:");
            items.forEach(item -> {
                answer.append("\n- ").append(item.path("class").asText("Lớp học"))
                        .append(": ").append(formatMoney(item.path("amount")))
                        .append(" · ").append(formatTuitionStatus(item.path("status").asText()));
                if (!item.path("dueDate").asText("").isBlank()) answer.append(" · hạn ").append(item.path("dueDate").asText());
            });
        } else answer.append("\nKhông có khoản học phí được ghi nhận trong tháng này.");
        return answer.toString();
    }

    private String formatParentHomework(com.fasterxml.jackson.databind.JsonNode data) {
        var assignments = data.path("assignments");
        if (!assignments.isArray() || assignments.isEmpty()) return "Hiện chưa có bài tập được giao cho con.";
        StringBuilder answer = new StringBuilder("Bài tập của con:");
        assignments.forEach(item -> answer.append("\n- ").append(item.path("title").asText("Bài tập"))
                .append(" · ").append(item.path("class").asText("Lớp học"))
                .append(" · hạn ").append(item.path("dueAt").asText())
                .append(" · ").append(item.path("status").asText())
                .append(" · điểm: ").append(item.path("score").asText("Chưa chấm")));
        return answer.toString();
    }

    private String formatMoney(com.fasterxml.jackson.databind.JsonNode value) {
        try {
            return java.text.NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"))
                    .format(new BigDecimal(value.asText("0"))) + " đồng";
        } catch (NumberFormatException e) { return "Không có dữ liệu"; }
    }

    private String formatTuitionStatus(String status) {
        return switch (status) {
            case "PAID" -> "Đã thanh toán";
            case "PROCESSING" -> "Đang xử lý";
            case "UNPAID" -> "Chưa thanh toán";
            default -> status;
        };
    }

    @Override
    public String sanitizeText(Long userId, String role, Long contextId, String text) {
        return redactText(text, personalNames(userId, role));
    }

    @Override
    public List<String> sanitizeTexts(Long userId, String role, Long contextId, List<String> texts) {
        List<String> names = personalNames(userId, role);
        return texts.stream().map(text -> redactText(text, names)).toList();
    }

    private List<String> personalNames(Long userId, String role) {
        List<String> names = new ArrayList<>();
        userRepository.findById(userId).map(User::getFullName).ifPresent(names::add);
        if ("ROLE_PARENT".equals(role)) {
            parentRepository.findByUserId(userId).ifPresent(parent ->
                    studentRepository.findByParentId(parent.getId()).forEach(s -> names.add(s.getFullName())));
        } else if ("ROLE_TEACHER".equals(role)) {
            classRepository.findAll().stream().filter(c ->
                            c.getTeacher() != null && c.getTeacher().getUser() != null && userId.equals(c.getTeacher().getUser().getId())
                                    || c.getAssistantTeacher() != null && c.getAssistantTeacher().getUser() != null
                                    && userId.equals(c.getAssistantTeacher().getUser().getId()))
                    .forEach(c -> classStudentRepository.findByClassEntityId(c.getId())
                            .forEach(enrollment -> names.add(enrollment.getStudent().getFullName())));
        } else if ("ROLE_ADMIN".equals(role)) {
            studentRepository.findAll().forEach(s -> names.add(s.getFullName()));
        }
        return names.stream().filter(n -> n != null && n.trim().length() >= 3)
                .map(String::trim).distinct().sorted(Comparator.comparingInt(String::length).reversed()).toList();
    }

    private String redactText(String text, List<String> names) {
        String sanitized = text == null ? "" : text;
        for (String name : names) {
            sanitized = replaceIgnoreCase(sanitized, java.util.regex.Pattern.quote(name), "[thông tin đã ẩn]");
        }
        sanitized = sanitized.replaceAll("(?i)[\\w.%+-]+@[\\w.-]+\\.[A-Z]{2,}", "[email đã ẩn]");
        sanitized = sanitized.replaceAll("(?<!\\d)(?:\\+?84|0)(?:[ .()-]*\\d){8,10}(?!\\d)", "[số điện thoại đã ẩn]");
        sanitized = sanitized.replaceAll("(?i)(địa chỉ|address)\\s*[:=]\\s*[^\\n,;]+", "$1: [địa chỉ đã ẩn]");
        sanitized = sanitized.replaceAll("(?i)(CCCD|CMND|mã học sinh|student id)\\s*[:#]?\\s*[A-Z0-9-]{5,}", "$1 [mã đã ẩn]");
        return sanitized;
    }

    private String replaceIgnoreCase(String source, String quotedPattern, String replacement) {
        return java.util.regex.Pattern.compile("(?i)" + quotedPattern)
                .matcher(source).replaceAll(java.util.regex.Matcher.quoteReplacement(replacement));
    }

    private Map<String, Object> adminData(String topic, YearMonth month, Map<String, String> replacements) {
        LocalDateTime from = month.atDay(1).atStartOfDay(), to = month.atEndOfMonth().atTime(LocalTime.MAX);
        return switch (topic) {
            case "ADMIN_OVERVIEW" -> Map.of("month", month.toString(),
                    "activeStudents", studentRepository.findByActiveTrue().size(),
                    "classes", classRepository.count(), "teachers", teacherRepository.count(),
                    "registrations", registrationRepository.countByRegistrationDateBetween(from, to),
                    "pendingRegistrations", registrationRepository.countByStatus("PENDING"));
            case "ADMIN_FINANCE" -> financeData(month);
            case "ADMIN_CLASS_SIZES" -> classSizes();
            case "ADMIN_REPORT_STATUS" -> reportStatus(month);
            case "ADMIN_NEW_STUDENTS" -> Map.of("month", month.toString(),
                    "newStudents", studentRepository.countByCreatedAtBetween(from, to));
            case "STUDENT_PROGRESS" -> progressData(classRepository.findAll(), month, replacements);
            default -> Map.of("message", "Không có dữ liệu tương ứng trong hệ thống.");
        };
    }

    private Map<String, Object> financeData(YearMonth month) {
        List<Tuition> allTuition = tuitionRepository.findAll();
        BigDecimal billed = allTuition.stream()
                .filter(t -> sameMonth(t.getBillingPeriod(), month))
                .map(Tuition::getAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paidTuition = allTuition.stream()
                .filter(t -> "PAID".equals(t.getStatus()) && sameMonth(t.getBillingPeriod(), month))
                .map(Tuition::getAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal outstanding = allTuition.stream()
                .filter(t -> ("UNPAID".equals(t.getStatus()) || "PROCESSING".equals(t.getStatus())) && sameMonth(t.getBillingPeriod(), month))
                .map(Tuition::getAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        YearMonth previousMonth = month.minusMonths(1);
        BigDecimal previousPaid = allTuition.stream().filter(t -> "PAID".equals(t.getStatus()) && sameMonth(t.getBillingPeriod(), previousMonth))
                .map(Tuition::getAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Map<String, BigDecimal>> byClass = new LinkedHashMap<>();
        allTuition.stream().filter(t -> sameMonth(t.getBillingPeriod(), month)).forEach(t -> {
            String className = t.getClassEntity().getName();
            Map<String, BigDecimal> values = byClass.computeIfAbsent(className, key -> new LinkedHashMap<>(Map.of("billed", BigDecimal.ZERO, "paid", BigDecimal.ZERO)));
            values.put("billed", values.get("billed").add(t.getAmount() == null ? BigDecimal.ZERO : t.getAmount()));
            if ("PAID".equals(t.getStatus())) values.put("paid", values.get("paid").add(t.getAmount() == null ? BigDecimal.ZERO : t.getAmount()));
        });
        LocalDateTime from = month.atDay(1).atStartOfDay(), to = month.atEndOfMonth().atTime(LocalTime.MAX);
        BigDecimal successfulPayments = paymentRepository.findAll().stream()
                .filter(p -> "SUCCESS".equals(p.getStatus()) && p.getPaymentDate() != null
                        && !p.getPaymentDate().isBefore(from) && !p.getPaymentDate().isAfter(to))
                .map(Payment::getAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        return Map.of("month", month.toString(), "previousMonth", previousMonth.toString(), "tuitionBilled", billed, "tuitionMarkedPaid", paidTuition,
                "outstandingTuition", outstanding, "successfulPaymentTransactions", successfulPayments,
                "previousMonthTuitionMarkedPaid", previousPaid, "byClass", byClass,
                "note", "Số liệu được tính từ các khoản Tuition và Payment hiện có; kỳ chưa gán được loại trừ khỏi tổng theo tháng.");
    }

    private Map<String, Object> classSizes() {
        Map<Long, Long> sizes = classStudentRepository.findAll().stream()
                .filter(e -> "ACTIVE".equals(e.getStatus()))
                .collect(Collectors.groupingBy(e -> e.getClassEntity().getId(), Collectors.counting()));
        List<Map<String, Object>> classes = classRepository.findAll().stream()
                .map(c -> Map.<String, Object>of("class", c.getName(), "size", sizes.getOrDefault(c.getId(), 0L)))
                .sorted(Comparator.comparingLong((Map<String, Object> row) -> (Long) row.get("size")).reversed())
                .toList();
        return Map.of("classes", classes);
    }

    private Map<String, Object> reportStatus(YearMonth month) {
        List<ClassSession> sessions = classSessionRepository.findAll().stream()
                .filter(s -> !s.getSessionDate().isBefore(month.atDay(1)) && !s.getSessionDate().isAfter(month.atEndOfMonth()))
                .filter(s -> !"CANCELLED".equals(s.getStatus())).toList();
        List<Map<String, Object>> missing = sessions.stream().filter(s -> s.getPublishedAt() == null)
                .limit(100).map(s -> Map.<String, Object>of("class", s.getClassEntity().getName(),
                        "date", s.getSessionDate().toString(), "status", s.getStatus())).toList();
        long unpublishedCount = sessions.stream().filter(s -> s.getPublishedAt() == null).count();
        return Map.of("month", month.toString(), "sessions", sessions.size(), "unpublishedReports", unpublishedCount, "details", missing);
    }

    private Map<String, Object> teacherData(Long userId, Long classId, String topic, YearMonth month,
                                            Map<String, String> replacements) {
        List<ClassEntity> assigned = classRepository.findAll().stream().filter(c ->
                c.getTeacher() != null && c.getTeacher().getUser() != null && userId.equals(c.getTeacher().getUser().getId())
                        || c.getAssistantTeacher() != null && c.getAssistantTeacher().getUser() != null
                        && userId.equals(c.getAssistantTeacher().getUser().getId())).toList();
        if (classId == null) return Map.of("message", "Hãy chọn lớp được phân công để tra cứu.",
                "assignedClasses", assigned.stream().map(ClassEntity::getName).toList());
        ClassEntity selected = assigned.stream().filter(c -> c.getId().equals(classId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Bạn không được phân công lớp này."));
        return switch (topic) {
            case "LEARNING_SUMMARY", "STUDENT_PROGRESS" -> progressData(List.of(selected), month, replacements);
            case "SCHEDULE" -> scheduleForClass(selected, month);
            case "HOMEWORK" -> homeworkForClass(selected);
            default -> Map.of("message", "Loại dữ liệu này không khả dụng cho người dạy.");
        };
    }

    private Map<String, Object> parentData(Long userId, Long studentId, String topic, YearMonth month) {
        Parent parent = parentRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hồ sơ phụ huynh."));
        List<Student> children = studentRepository.findByParentId(parent.getId());
        if (studentId == null) return Map.of("message", "Hãy chọn học sinh để tra cứu.",
                "childCount", children.size(), "availableChildren", children.size());
        Student selected = children.stream().filter(s -> s.getId().equals(studentId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Học sinh không thuộc tài khoản phụ huynh này."));
        return switch (topic) {
            case "LEARNING_SUMMARY", "STUDENT_PROGRESS" -> parentLearning(selected, month,
                    userRepository.findById(userId).map(User::getFullName).orElse(""));
            case "SCHEDULE" -> parentSchedule(selected);
            case "TUITION" -> parentTuition(selected, month);
            case "HOMEWORK" -> parentHomework(userId, selected);
            default -> Map.of("message", "Loại dữ liệu này không khả dụng cho phụ huynh.");
        };
    }

    private Map<String, Object> progressData(List<ClassEntity> classes, YearMonth month,
                                             Map<String, String> replacements) {
        Set<Long> ids = classes.stream().map(ClassEntity::getId).collect(Collectors.toSet());
        LocalDateTime from = month.atDay(1).atStartOfDay(), to = month.atEndOfMonth().atTime(LocalTime.MAX);
        Map<Long, List<SessionStudentReport>> perStudent = reportRepository.findAll().stream()
                .filter(r -> ids.contains(r.getClassSession().getClassEntity().getId()))
                .filter(r -> !r.getClassSession().getSessionDate().isBefore(month.atDay(1))
                        && !r.getClassSession().getSessionDate().isAfter(month.atEndOfMonth()))
                .collect(Collectors.groupingBy(r -> r.getStudent().getId()));
        List<Map<String, Object>> results = new ArrayList<>();
        int index = 1;
        for (Map.Entry<Long, List<SessionStudentReport>> entry : perStudent.entrySet()) {
            String alias = "HOC_SINH_" + index++;
            Student student = entry.getValue().get(0).getStudent();
            replacements.put(alias, student.getFullName());
            List<SessionStudentReport> rows = entry.getValue();
            BigDecimal homeworkAverage = average(rows.stream().map(SessionStudentReport::getHomeworkScore).filter(Objects::nonNull).toList());
            long present = rows.stream().filter(r -> "PRESENT".equals(r.getAttendanceStatus())).count();
            long absent = rows.stream().filter(r -> "ABSENT".equals(r.getAttendanceStatus())).count();
            results.add(Map.of("student", alias, "class", rows.get(0).getClassSession().getClassEntity().getName(),
                    "reportedSessions", rows.size(), "present", present, "absent", absent,
                    "homeworkAverage", homeworkAverage == null ? "Chưa có điểm" : homeworkAverage,
                    "trend", scoreTrend(rows)));
        }
        return Map.of("month", month.toString(), "students", results,
                "note", "Tên học sinh được ẩn danh trong dữ liệu gửi đến mô hình.");
    }

    private Map<String, Object> parentLearning(Student student, YearMonth month, String parentName) {
        List<Grade> grades = gradeRepository.findByStudentId(student.getId());
        List<Attendance> attendance = attendanceRepository.findByStudentIdOrderByAttendanceDateDesc(student.getId()).stream()
                .filter(a -> YearMonth.from(a.getAttendanceDate()).equals(month)).toList();
        List<SessionStudentReport> reports = reportRepository.findAll().stream()
                .filter(r -> r.getStudent().getId().equals(student.getId()) && r.getClassSession().getPublishedAt() != null
                        && YearMonth.from(r.getClassSession().getSessionDate()).equals(month)).toList();
        BigDecimal gradeAverage = average(grades.stream().map(Grade::getAverageScore).filter(Objects::nonNull).toList());
        BigDecimal homeworkAverage = average(reports.stream().map(SessionStudentReport::getHomeworkScore).filter(Objects::nonNull).toList());
        long present = reports.stream().filter(r -> "PRESENT".equals(r.getAttendanceStatus())).count();
        long absent = reports.stream().filter(r -> "ABSENT".equals(r.getAttendanceStatus())).count();
        return Map.of("month", month.toString(), "gradeAverages", grades.stream().map(g -> Map.of(
                        "subject", g.getClassEntity().getSubject().getName(),
                        "average", g.getAverageScore() == null ? "Chưa đủ cột điểm" : g.getAverageScore())).toList(),
                "overallGradeAverage", gradeAverage == null ? "Chưa đủ dữ liệu" : gradeAverage,
                "homeworkAverage", homeworkAverage == null ? "Chưa có điểm" : homeworkAverage,
                "publishedPresent", present, "publishedAbsent", absent,
                "teacherComments", reports.stream().map(SessionStudentReport::getComment).filter(Objects::nonNull)
                        .map(comment -> redactComment(comment, student.getFullName(), parentName)).limit(12).toList());
    }

    private Map<String, Object> scheduleForClass(ClassEntity entity, YearMonth month) {
        List<Map<String, Object>> sessions = classSessionRepository.findByClassEntityIdAndSessionDateBetweenOrderBySessionDateAscStartTimeAsc(
                        entity.getId(), month.atDay(1), month.atEndOfMonth()).stream()
                .filter(s -> !"CANCELLED".equals(s.getStatus()))
                .map(s -> Map.<String, Object>of("date", s.getSessionDate().toString(), "start", s.getStartTime().toString(), "end", s.getEndTime().toString(), "status", s.getStatus()))
                .toList();
        return Map.of("class", entity.getName(), "month", month.toString(), "sessions", sessions);
    }

    private Map<String, Object> parentSchedule(Student student) {
        List<ClassStudent> enrollments = classStudentRepository.findByStudentId(student.getId()).stream()
                .filter(e -> "ACTIVE".equals(e.getStatus())).toList();
        LocalDate today = LocalDate.now(APP_ZONE);
        List<Map<String, Object>> upcoming = new ArrayList<>();
        for (ClassStudent enrollment : enrollments) {
            classSessionRepository.findUpcomingForClass(enrollment.getClassEntity().getId(), today, LocalTime.now(APP_ZONE))
                    .stream().limit(3).forEach(s -> upcoming.add(Map.of("class", enrollment.getClassEntity().getName(),
                            "date", s.getSessionDate().toString(), "start", s.getStartTime().toString(), "end", s.getEndTime().toString())));
        }
        return Map.of("upcomingSessions", upcoming.stream().limit(20).toList());
    }

    private Map<String, Object> parentTuition(Student student, YearMonth month) {
        List<Tuition> items = tuitionRepository.findByStudentId(student.getId());
        BigDecimal paid = items.stream().filter(t -> "PAID".equals(t.getStatus()) && sameMonth(t.getBillingPeriod(), month))
                .map(Tuition::getAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal due = items.stream().filter(t -> ("UNPAID".equals(t.getStatus()) || "PROCESSING".equals(t.getStatus())) && sameMonth(t.getBillingPeriod(), month))
                .map(Tuition::getAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        return Map.of("month", month.toString(), "paid", paid, "unpaidOrProcessing", due,
                "items", items.stream().filter(t -> sameMonth(t.getBillingPeriod(), month)).map(t -> Map.of(
                        "class", t.getClassEntity().getName(), "amount", t.getAmount(), "status", t.getStatus(),
                        "dueDate", t.getDueDate() == null ? "" : t.getDueDate().toString())).toList());
    }

    private Map<String, Object> homeworkForClass(ClassEntity entity) {
        List<HomeworkAssignment> assignments = homeworkAssignmentRepository.findByClassEntityIdInOrderByDueAtDesc(List.of(entity.getId()));
        return Map.of("class", entity.getName(), "assignments", assignments.stream().limit(30).map(a -> Map.of(
                "title", a.getTitle(), "instructions", a.getInstructions(), "dueAt", a.getDueAt().toString(),
                "submissions", homeworkSubmissionRepository.findByAssignmentIdOrderBySubmittedAtDesc(a.getId()).size())).toList());
    }

    private Map<String, Object> parentHomework(Long userId, Student student) {
        List<Long> classIds = classStudentRepository.findByStudentId(student.getId()).stream()
                .filter(e -> "ACTIVE".equals(e.getStatus())).map(e -> e.getClassEntity().getId()).toList();
        if (classIds.isEmpty()) return Map.of("assignments", List.of());
        List<HomeworkAssignment> assignments = homeworkAssignmentRepository.findByClassEntityIdInAndPublishedAtLessThanEqualOrderByDueAtAsc(classIds, LocalDateTime.now(APP_ZONE));
        List<HomeworkSubmission> submissions = homeworkSubmissionRepository.findByStudentParentUserIdAndAssignmentIdIn(
                userId, assignments.stream().map(HomeworkAssignment::getId).toList());
        Map<Long, HomeworkSubmission> byAssignment = submissions.stream().collect(Collectors.toMap(s -> s.getAssignment().getId(), s -> s, (a, b) -> a));
        return Map.of("assignments", assignments.stream().limit(40).map(a -> {
            HomeworkSubmission submission = byAssignment.get(a.getId());
            return Map.<String, Object>of("class", a.getClassEntity().getName(), "title", a.getTitle(),
                    "dueAt", a.getDueAt().toString(), "status", submission == null ? "Chưa nộp" : submission.getStatus(),
                    "score", submission == null || submission.getScore() == null ? "Chưa chấm" : submission.getScore());
        }).toList());
    }

    private BigDecimal average(List<BigDecimal> values) {
        return values.isEmpty() ? null : values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 2, java.math.RoundingMode.HALF_UP);
    }

    private String scoreTrend(List<SessionStudentReport> rows) {
        List<BigDecimal> scores = rows.stream().filter(r -> r.getHomeworkScore() != null)
                .sorted(Comparator.comparing(r -> r.getClassSession().getSessionDate()))
                .map(SessionStudentReport::getHomeworkScore).toList();
        if (scores.size() < 2) return "Chưa đủ dữ liệu so sánh";
        int compare = scores.get(scores.size() - 1).compareTo(scores.get(0));
        return compare > 0 ? "Tăng" : compare < 0 ? "Giảm" : "Ổn định";
    }

    private boolean sameMonth(LocalDate date, YearMonth month) { return date != null && YearMonth.from(date).equals(month); }
    private String redactComment(String comment, String studentName, String parentName) {
        String value = comment;
        for (String name : List.of(studentName == null ? "" : studentName, parentName == null ? "" : parentName)) {
            if (name.length() >= 3) value = java.util.regex.Pattern.compile(java.util.regex.Pattern.quote(name), java.util.regex.Pattern.CASE_INSENSITIVE)
                    .matcher(value).replaceAll("[người liên quan]");
        }
        value = value.replaceAll("(?i)[\\w.%+-]+@[\\w.-]+\\.[A-Z]{2,}", "[email đã ẩn]")
                .replaceAll("(?<!\\d)(?:\\+?84|0)(?:[ .()-]*\\d){8,10}(?!\\d)", "[số điện thoại đã ẩn]")
                .replaceAll("(?i)(địa chỉ|address)\\s*[:=]\\s*[^\\n,;]+", "$1: [địa chỉ đã ẩn]");
        return value;
    }
    private YearMonth parseMonth(String period) {
        try { return period == null || period.isBlank() ? YearMonth.now(APP_ZONE) : YearMonth.parse(period); }
        catch (Exception e) { throw new IllegalArgumentException("Tháng phải có định dạng YYYY-MM."); }
    }
}
