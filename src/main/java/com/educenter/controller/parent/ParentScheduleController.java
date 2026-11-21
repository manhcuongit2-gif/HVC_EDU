package com.educenter.controller.parent;

import com.educenter.dto.ParentCalendarDay;
import com.educenter.dto.ParentChildAttendance;
import com.educenter.dto.ParentScheduleEvent;
import com.educenter.entity.ClassSession;
import com.educenter.entity.ClassStudent;
import com.educenter.entity.Parent;
import com.educenter.entity.Student;
import com.educenter.repository.ClassSessionRepository;
import com.educenter.repository.ClassStudentRepository;
import com.educenter.repository.AttendanceRepository;
import com.educenter.repository.ParentRepository;
import com.educenter.repository.SessionStudentReportRepository;
import com.educenter.repository.StudentRepository;
import com.educenter.security.CustomUserDetails;
import com.educenter.service.ClassSessionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.*;

@Controller
@RequestMapping("/parent/schedule")
public class ParentScheduleController {
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final List<String> COLORS = List.of("#2563eb", "#0f9d78", "#8b5cf6", "#e77922", "#d04474", "#087f9b");

    private final ParentRepository parentRepository;
    private final StudentRepository studentRepository;
    private final ClassStudentRepository classStudentRepository;
    private final ClassSessionRepository classSessionRepository;
    private final ClassSessionService classSessionService;
    private final AttendanceRepository attendanceRepository;
    private final SessionStudentReportRepository sessionReports;

    public ParentScheduleController(ParentRepository parentRepository, StudentRepository studentRepository,
                                    ClassStudentRepository classStudentRepository, ClassSessionRepository classSessionRepository,
                                    ClassSessionService classSessionService, AttendanceRepository attendanceRepository,
                                    SessionStudentReportRepository sessionReports) {
        this.parentRepository = parentRepository;
        this.studentRepository = studentRepository;
        this.classStudentRepository = classStudentRepository;
        this.classSessionRepository = classSessionRepository;
        this.classSessionService = classSessionService;
        this.attendanceRepository = attendanceRepository;
        this.sessionReports = sessionReports;
    }

    @GetMapping
    public String index(@AuthenticationPrincipal CustomUserDetails user,
                        @RequestParam(required = false) String month,
                        @RequestParam(required = false) Long studentId,
                        Model model) {
        YearMonth selectedMonth = parseMonth(month);
        Parent parent = parentRepository.findByUserId(user.getId()).orElse(null);
        List<Student> children = parent == null ? List.of() : studentRepository.findByParentId(parent.getId());
        if (studentId != null) {
            boolean childIsOwned = false;
            for (Student child : children) if (child.getId().equals(studentId)) childIsOwned = true;
            if (!childIsOwned) studentId = null;
        }
        final Long selectedChildId = studentId;
        if (!children.isEmpty()) classSessionService.synchronizeAllClasses();

        LocalDate firstOfMonth = selectedMonth.atDay(1);
        LocalDate lastOfMonth = selectedMonth.atEndOfMonth();
        LocalDate gridStart = firstOfMonth.minusDays(firstOfMonth.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue());
        LocalDate gridEnd = lastOfMonth.plusDays(DayOfWeek.SUNDAY.getValue() - lastOfMonth.getDayOfWeek().getValue());
        LocalDate today = LocalDate.now(APP_ZONE);
        Map<Long, EventAccumulator> events = new LinkedHashMap<>();
        Set<Long> seenClassIds = new LinkedHashSet<>();

        for (Student child : children) {
            if (selectedChildId != null && !selectedChildId.equals(child.getId())) continue;
            Map<Long, String> publishedStatusBySession = new HashMap<>();
            sessionReports.findPublishedForStudent(child.getId()).forEach(report ->
                    publishedStatusBySession.put(report.getClassSession().getId(), report.getAttendanceStatus()));
            Map<String, String> storedStatusByClassDate = new HashMap<>();
            attendanceRepository.findByStudentIdOrderByAttendanceDateDesc(child.getId()).forEach(row ->
                    storedStatusByClassDate.put(row.getClassEntity().getId() + ":" + row.getAttendanceDate(), row.getStatus()));
            for (ClassStudent enrollment : classStudentRepository.findByStudentId(child.getId())) {
                if (!"ACTIVE".equals(enrollment.getStatus()) || enrollment.getClassEntity() == null) continue;
                if (enrollment.getEnrolledDate() != null && enrollment.getEnrolledDate().isAfter(lastOfMonth)) continue;
                Long classId = enrollment.getClassEntity().getId();
                seenClassIds.add(classId);
                for (ClassSession session : classSessionRepository.findByClassEntityIdAndSessionDateBetweenOrderBySessionDateAscStartTimeAsc(classId, gridStart, gridEnd)) {
                    EventAccumulator accumulator = events.computeIfAbsent(session.getId(), ignored -> new EventAccumulator(session));
                    accumulator.childNames.add(child.getFullName());
                    String attendanceStatus = publishedStatusBySession.get(session.getId());
                    if (attendanceStatus == null) {
                        attendanceStatus = storedStatusByClassDate.get(classId + ":" + session.getSessionDate());
                    }
                    accumulator.attendance.put(child.getId(), new ParentChildAttendance(child.getFullName(), attendanceStatus));
                }
            }
        }

        Map<Long, String> classColors = new HashMap<>();
        int colorIndex = 0;
        for (Long classId : seenClassIds) classColors.put(classId, COLORS.get(colorIndex++ % COLORS.size()));
        Map<LocalDate, List<ParentScheduleEvent>> eventsByDate = new HashMap<>();
        events.values().forEach(value -> {
            String color = classColors.getOrDefault(value.session.getClassEntity().getId(), COLORS.get(0));
            ParentScheduleEvent event = new ParentScheduleEvent(value.session, List.copyOf(value.childNames),
                    List.copyOf(value.attendance.values()),
                    value.session.getPublishedAt() != null, color);
            eventsByDate.computeIfAbsent(value.session.getSessionDate(), ignored -> new ArrayList<>()).add(event);
        });

        List<ParentCalendarDay> days = new ArrayList<>();
        for (LocalDate date = gridStart; !date.isAfter(gridEnd); date = date.plusDays(1)) {
            List<ParentScheduleEvent> dayEvents = eventsByDate.getOrDefault(date, List.of()).stream()
                    .sorted(Comparator.comparing((ParentScheduleEvent e) -> e.session().getStartTime())).toList();
            days.add(new ParentCalendarDay(date, YearMonth.from(date).equals(selectedMonth), date.equals(today), dayEvents));
        }

        model.addAttribute("currentUser", user);
        model.addAttribute("children", children);
        model.addAttribute("selectedStudentId", studentId);
        model.addAttribute("selectedMonth", selectedMonth.toString());
        model.addAttribute("monthLabel", selectedMonth.getMonth().getValue() + "/" + selectedMonth.getYear());
        model.addAttribute("previousMonth", selectedMonth.minusMonths(1));
        model.addAttribute("nextMonth", selectedMonth.plusMonths(1));
        model.addAttribute("todayMonth", YearMonth.from(today).toString());
        model.addAttribute("calendarDays", days);
        return "parent/schedule";
    }

    private YearMonth parseMonth(String value) {
        if (value != null) {
            try { return YearMonth.parse(value); } catch (DateTimeParseException ignored) { }
        }
        return YearMonth.from(LocalDate.now(APP_ZONE));
    }

    private static final class EventAccumulator {
        private final ClassSession session;
        private final Set<String> childNames = new LinkedHashSet<>();
        private final Map<Long, ParentChildAttendance> attendance = new LinkedHashMap<>();
        private EventAccumulator(ClassSession session) { this.session = session; }
    }
}
