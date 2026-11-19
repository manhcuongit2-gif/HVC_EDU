package com.educenter.controller.teacher;

import com.educenter.entity.ClassEntity;
import com.educenter.entity.ClassSession;
import com.educenter.repository.ClassRepository;
import com.educenter.security.CustomUserDetails;
import com.educenter.service.ClassSessionService;
import com.educenter.service.StudentReportService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

@Controller
@RequestMapping("/teacher/reports")
public class TeacherReportController {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final ClassRepository classRepository;
    private final ClassSessionService classSessionService;
    private final StudentReportService studentReportService;

    public TeacherReportController(ClassRepository classRepository, ClassSessionService classSessionService,
                                   StudentReportService studentReportService) {
        this.classRepository = classRepository;
        this.classSessionService = classSessionService;
        this.studentReportService = studentReportService;
    }

    @GetMapping
    public String index(@AuthenticationPrincipal CustomUserDetails user, @RequestParam(required = false) String month,
                        @RequestParam(required = false) Long classId, Model model) {
        YearMonth selectedMonth = parseMonth(month);
        LocalDate from = selectedMonth.atDay(1);
        LocalDate to = selectedMonth.atEndOfMonth();
        List<ClassEntity> classes = classRepository.findByTeacherUserId(user.getId());
        List<ClassSession> sessions = classSessionService.findForTeacher(user.getId(), from, to);
        model.addAttribute("currentUser", user);
        model.addAttribute("month", selectedMonth.toString());
        model.addAttribute("previousMonth", selectedMonth.minusMonths(1));
        model.addAttribute("nextMonth", selectedMonth.plusMonths(1));
        model.addAttribute("classes", classes);
        model.addAttribute("sessions", sessions);
        model.addAttribute("selectedClassId", classId);
        if (classId != null && classes.stream().anyMatch(c -> c.getId().equals(classId))) {
            model.addAttribute("monthlyReports", studentReportService.getMonthlyReports(classId, selectedMonth, user.getId()));
        }
        return "teacher/reports";
    }

    @GetMapping("/session/{id}")
    public String session(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user,
                          Model model, RedirectAttributes redirect) {
        try {
            ClassSession session = studentReportService.getTeacherSession(id, user.getId());
            model.addAttribute("currentUser", user);
            model.addAttribute("classSession", session);
            model.addAttribute("reports", studentReportService.getTeacherSessionReports(id, user.getId()));
            return "teacher/session-report";
        } catch (Exception e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/teacher/reports";
        }
    }

    @PostMapping("/session/{id}")
    public String saveSession(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user,
                              @RequestParam(required = false) List<Long> studentIds,
                              @RequestParam(required = false) List<String> attendanceStatuses,
                              @RequestParam(required = false) List<String> homeworkScores,
                              @RequestParam(required = false) List<String> comments,
                              @RequestParam(required = false) List<String> goodEvaluationCounts,
                              @RequestParam(required = false) List<String> notes,
                              @RequestParam(defaultValue = "false") boolean publish,
                              RedirectAttributes redirect) {
        try {
            studentReportService.saveSessionReports(id, user.getId(), emptyIfNull(studentIds), emptyIfNull(attendanceStatuses),
                    emptyIfNull(homeworkScores), emptyIfNull(comments), emptyIfNull(goodEvaluationCounts), emptyIfNull(notes));
            if (publish) studentReportService.publishSession(id, user.getId());
            redirect.addFlashAttribute("successMessage", publish ? "Đã công bố báo cáo buổi học cho phụ huynh." : "Đã lưu bản nháp báo cáo.");
        } catch (Exception e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/teacher/reports/session/" + id;
    }

    @PostMapping("/monthly/{classId}/{studentId}")
    public String publishMonthly(@PathVariable Long classId, @PathVariable Long studentId,
                                 @RequestParam String month, @RequestParam String teacherComment,
                                 @AuthenticationPrincipal CustomUserDetails user, RedirectAttributes redirect) {
        YearMonth selectedMonth = parseMonth(month);
        try {
            boolean billed = studentReportService.publishMonthlyReport(classId, studentId, selectedMonth, teacherComment, user.getId());
            redirect.addFlashAttribute("successMessage", billed ? "Đã công bố báo cáo tháng và đồng bộ học phí."
                    : "Đã công bố báo cáo tháng; học phí chưa được tạo vì lớp thiếu đơn giá hoặc còn khoản thu cũ chưa gắn kỳ.");
        } catch (Exception e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/teacher/reports?month=" + selectedMonth + "&classId=" + classId;
    }

    private YearMonth parseMonth(String value) {
        try {
            return value == null || value.isBlank() ? YearMonth.now(APP_ZONE) : YearMonth.parse(value);
        } catch (RuntimeException e) {
            return YearMonth.now(APP_ZONE);
        }
    }

    private <T> List<T> emptyIfNull(List<T> values) {
        return values == null ? List.of() : values;
    }
}
