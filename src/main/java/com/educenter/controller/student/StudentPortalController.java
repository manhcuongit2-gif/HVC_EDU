package com.educenter.controller.student;

import com.educenter.dto.StudentHomeworkRow;
import com.educenter.dto.StudentCalendarDay;
import com.educenter.dto.StudentScheduleEvent;
import com.educenter.entity.ClassSession;
import com.educenter.entity.ClassStudent;
import com.educenter.repository.ClassSessionRepository;
import com.educenter.security.CustomUserDetails;
import com.educenter.service.HomeworkService;
import com.educenter.service.ExamService;
import com.educenter.service.StudentPortalService;
import com.educenter.service.impl.PdfExamService;
import com.educenter.service.impl.StudentLearningProgressService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.*;

@Controller
@RequestMapping("/student")
@PreAuthorize("hasRole('STUDENT')")
public class StudentPortalController {
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final List<String> SCHEDULE_COLORS = List.of("#2563eb", "#0f9d78", "#8b5cf6", "#e77922", "#d04474", "#087f9b");
    private final StudentPortalService portal;
    private final HomeworkService homework;
    private final ExamService exams;
    private final PdfExamService pdfExams;
    private final StudentLearningProgressService learningProgress;
    private final ClassSessionRepository sessions;

    public StudentPortalController(StudentPortalService portal, HomeworkService homework, ExamService exams,
                                   PdfExamService pdfExams, StudentLearningProgressService learningProgress,
                                   ClassSessionRepository sessions) {
        this.portal = portal;
        this.homework = homework;
        this.exams = exams;
        this.pdfExams = pdfExams;
        this.learningProgress = learningProgress;
        this.sessions = sessions;
    }

    @GetMapping({"", "/dashboard"})
    public String dashboard(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        model.addAttribute("currentUser", user);
        model.addAttribute("portal", portal.getPortalData(user.getId()));
        model.addAttribute("examAssignments", exams.assignmentsForStudent(user.getId()));
        model.addAttribute("pdfExamAssignments", pdfExams.forStudent(user.getId()));
        return "student/dashboard";
    }

    @GetMapping("/progress")
    public String progress(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        model.addAttribute("currentUser", user);
        var portalData = portal.getPortalData(user.getId());
        model.addAttribute("portal", portalData);
        model.addAttribute("learningProgress", learningProgress.overview(user.getId(), portalData));
        model.addAttribute("examResults", exams.publishedResultsForStudent(user.getId()));
        return "student/progress";
    }

    @GetMapping("/schedule")
    public String schedule(@AuthenticationPrincipal CustomUserDetails user,
                           @RequestParam(required = false) String month, Model model) {
        YearMonth selectedMonth = parseMonth(month);
        var portalData = portal.getPortalData(user.getId());
        LocalDate first = selectedMonth.atDay(1);
        LocalDate last = selectedMonth.atEndOfMonth();
        LocalDate gridStart = first.minusDays(first.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue());
        LocalDate gridEnd = last.plusDays(DayOfWeek.SUNDAY.getValue() - last.getDayOfWeek().getValue());
        Map<String, com.educenter.dto.StudentAttendanceRow> attendanceByClassDate = new HashMap<>();
        portalData.attendance().forEach(row -> attendanceByClassDate.put(row.classEntity().getId() + ":" + row.date(), row));

        Map<Long, ClassSession> byId = new LinkedHashMap<>();
        int colorIndex = 0;
        Map<Long, String> colors = new HashMap<>();
        for (ClassStudent enrollment : portalData.activeEnrollments()) {
            Long classId = enrollment.getClassEntity().getId();
            colors.putIfAbsent(classId, SCHEDULE_COLORS.get(colorIndex++ % SCHEDULE_COLORS.size()));
            for (ClassSession session : sessions.findByClassEntityIdAndSessionDateBetweenOrderBySessionDateAscStartTimeAsc(classId, gridStart, gridEnd)) {
                byId.put(session.getId(), session);
            }
        }

        Map<LocalDate, List<StudentScheduleEvent>> eventsByDate = new HashMap<>();
        byId.values().forEach(session -> {
            var attendanceRow = attendanceByClassDate.get(session.getClassEntity().getId() + ":" + session.getSessionDate());
            eventsByDate.computeIfAbsent(session.getSessionDate(), ignored -> new ArrayList<>()).add(
                    new StudentScheduleEvent(session, attendanceRow == null ? null : attendanceRow.status(),
                            attendanceRow == null ? null : attendanceRow.note(), colors.get(session.getClassEntity().getId())));
        });
        List<StudentCalendarDay> calendarDays = new ArrayList<>();
        LocalDate today = LocalDate.now(APP_ZONE);
        for (LocalDate date = gridStart; !date.isAfter(gridEnd); date = date.plusDays(1)) {
            List<StudentScheduleEvent> dayEvents = eventsByDate.getOrDefault(date, List.of()).stream()
                    .sorted(Comparator.comparing(event -> event.session().getStartTime())).toList();
            calendarDays.add(new StudentCalendarDay(date, YearMonth.from(date).equals(selectedMonth), date.equals(today), dayEvents));
        }

        model.addAttribute("currentUser", user);
        model.addAttribute("portal", portalData);
        model.addAttribute("selectedMonth", selectedMonth);
        model.addAttribute("monthLabel", selectedMonth.getMonthValue() + "/" + selectedMonth.getYear());
        model.addAttribute("previousMonth", selectedMonth.minusMonths(1));
        model.addAttribute("nextMonth", selectedMonth.plusMonths(1));
        model.addAttribute("todayMonth", YearMonth.from(today));
        model.addAttribute("calendarDays", calendarDays);
        return "student/schedule";
    }

    private YearMonth parseMonth(String value) {
        if (value != null) {
            try { return YearMonth.parse(value); } catch (DateTimeParseException ignored) { }
        }
        return YearMonth.from(LocalDate.now(APP_ZONE));
    }

    @GetMapping("/homework")
    public String homework(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        model.addAttribute("currentUser", user);
        model.addAttribute("rows", homework.findForStudent(user.getId()));
        return "student/homework";
    }

    @PostMapping("/homework/{id}/submit")
    public String submit(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user,
                         @RequestParam(required = false) String responseText,
                         @RequestParam(required = false) MultipartFile file,
                         RedirectAttributes redirect) {
        try {
            var submission = homework.submitForStudent(user.getId(), id, responseText, file);
            redirect.addFlashAttribute("successMessage", submission.isLate()
                    ? "Đã nộp bài. Hệ thống ghi nhận bài nộp muộn."
                    : "Đã nộp bài thành công.");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/student/homework";
    }

    @GetMapping("/homework/{id}/attachment")
    public ResponseEntity<Resource> assignmentFile(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        StudentHomeworkRow row = homework.findForStudent(user.getId()).stream()
                .filter(item -> item.assignment().getId().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài tập."));
        return response(row.assignment().getAttachmentOriginalFilename(), homework.assignmentFileForStudent(user.getId(), id),
                row.assignment().getAttachmentContentType());
    }

    @GetMapping("/homework/submission/{id}/file")
    public ResponseEntity<Resource> submissionFile(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        StudentHomeworkRow row = homework.findForStudent(user.getId()).stream()
                .filter(item -> item.submission() != null && item.submission().getId().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài nộp."));
        return response(row.submission().getAttachmentOriginalFilename(), homework.submissionFileForStudent(user.getId(), id),
                row.submission().getAttachmentContentType());
    }

    private ResponseEntity<Resource> response(String filename, java.nio.file.Path path, String contentType) {
        MediaType media = contentType == null ? MediaTypeFactory.getMediaType(filename).orElse(MediaType.APPLICATION_OCTET_STREAM)
                : MediaType.parseMediaType(contentType);
        return ResponseEntity.ok().contentType(media).header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
                .body(new FileSystemResource(path));
    }
}
