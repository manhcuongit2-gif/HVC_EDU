package com.educenter.controller.teacher;

import com.educenter.dto.SessionReportImportPreview;
import com.educenter.dto.SessionReportImportRow;
import com.educenter.entity.ClassEntity;
import com.educenter.entity.ClassSession;
import com.educenter.entity.MonthlyStudentReport;
import com.educenter.entity.SessionStudentReport;
import com.educenter.repository.ClassRepository;
import com.educenter.security.CustomUserDetails;
import com.educenter.service.ClassSessionService;
import com.educenter.service.StudentReportService;
import com.educenter.service.ReportFileService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;

@Controller
@RequestMapping("/teacher/reports")
public class TeacherReportController {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final ClassRepository classRepository;
    private final ClassSessionService classSessionService;
    private final StudentReportService studentReportService;
    private final ReportFileService reportFileService;

    public TeacherReportController(ClassRepository classRepository, ClassSessionService classSessionService,
                                   StudentReportService studentReportService, ReportFileService reportFileService) {
        this.classRepository = classRepository;
        this.classSessionService = classSessionService;
        this.studentReportService = studentReportService;
        this.reportFileService = reportFileService;
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

    @GetMapping("/session/{id}/template.xlsx")
    public ResponseEntity<byte[]> downloadTemplate(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        try {
            ClassSession session = studentReportService.getTeacherSession(id, user.getId());
            List<SessionStudentReport> reports = studentReportService.getTeacherSessionReports(id, user.getId());
            String filename = "mau-bao-cao-buoi-" + id + ".xlsx";
            return download(reportFileService.createSessionTemplate(session, reports), filename,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        }
    }

    @PostMapping("/session/{id}/import/preview")
    public String previewImport(@PathVariable Long id, @RequestParam("file") MultipartFile file,
                                @AuthenticationPrincipal CustomUserDetails user, Model model,
                                RedirectAttributes redirect) {
        try {
            ClassSession session = studentReportService.getTeacherSession(id, user.getId());
            if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024) {
                throw new IllegalArgumentException("Chọn tệp Excel có dung lượng tối đa 5 MB.");
            }
            SessionReportImportPreview parsed;
            try (InputStream input = file.getInputStream()) {
                parsed = reportFileService.parseSessionWorkbook(input);
            }
            List<String> errors = new ArrayList<>(parsed.errors());
            List<SessionStudentReport> expectedReports = studentReportService.getTeacherSessionReports(id, user.getId());
            validateImportRoster(parsed.rows(), expectedReports, errors);
            SessionReportImportPreview preview = new SessionReportImportPreview(parsed.rows(), errors);
            model.addAttribute("currentUser", user);
            model.addAttribute("classSession", session);
            model.addAttribute("preview", preview);
            return "teacher/session-report-preview";
        } catch (Exception e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage() == null ? "Không thể đọc tệp Excel." : e.getMessage());
            return "redirect:/teacher/reports/session/" + id;
        }
    }

    @PostMapping("/session/{id}/import/confirm")
    public String confirmImport(@PathVariable Long id,
                                @RequestParam(required = false) List<Long> studentIds,
                                @RequestParam(required = false) List<String> attendanceStatuses,
                                @RequestParam(required = false) List<String> homeworkScores,
                                @RequestParam(required = false) List<String> comments,
                                @RequestParam(required = false) List<String> goodEvaluationCounts,
                                @RequestParam(required = false) List<String> notes,
                                @AuthenticationPrincipal CustomUserDetails user,
                                RedirectAttributes redirect) {
        try {
            List<Long> ids = emptyIfNull(studentIds);
            List<SessionStudentReport> expectedReports = studentReportService.getTeacherSessionReports(id, user.getId());
            if (ids.size() != expectedReports.size() || new HashSet<>(ids).size() != ids.size()) {
                throw new IllegalArgumentException("Danh sách học sinh trong bản xem trước đã thay đổi; vui lòng tải Excel lên lại.");
            }
            Set<Long> expectedIds = expectedReports.stream().map(r -> r.getStudent().getId()).collect(java.util.stream.Collectors.toSet());
            if (!expectedIds.equals(new HashSet<>(ids))) throw new IllegalArgumentException("Excel không khớp danh sách học sinh của buổi học.");
            studentReportService.saveSessionReports(id, user.getId(), ids, emptyIfNull(attendanceStatuses),
                    emptyIfNull(homeworkScores), emptyIfNull(comments), emptyIfNull(goodEvaluationCounts), emptyIfNull(notes));
            redirect.addFlashAttribute("successMessage", "Đã nhập Excel và lưu báo cáo thành bản nháp. Hãy kiểm tra rồi công bố khi sẵn sàng.");
        } catch (Exception e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/teacher/reports/session/" + id;
    }

    @GetMapping("/session/{id}/export")
    public ResponseEntity<byte[]> exportSession(@PathVariable Long id, @RequestParam(defaultValue = "xlsx") String format,
                                                @AuthenticationPrincipal CustomUserDetails user) {
        try {
            ClassSession session = studentReportService.getTeacherSession(id, user.getId());
            List<SessionStudentReport> reports = studentReportService.getTeacherSessionReports(id, user.getId());
            String title = "Báo cáo buổi học - " + session.getClassEntity().getName() + " - " + session.getSessionDate();
            byte[] file = "pdf".equalsIgnoreCase(format) ? reportFileService.exportSessionPdf(title, reports)
                    : "xlsx".equalsIgnoreCase(format) ? reportFileService.exportSessionExcel(title, reports) : null;
            return formatDownload(file, "bao-cao-buoi-" + id, format);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        }
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportMonthly(@RequestParam Long classId, @RequestParam(required = false) String month,
                                                @RequestParam(defaultValue = "xlsx") String format,
                                                @AuthenticationPrincipal CustomUserDetails user) {
        try {
            YearMonth selectedMonth = parseMonth(month);
            List<MonthlyStudentReport> reports = studentReportService.getMonthlyReports(classId, selectedMonth, user.getId());
            String title = "Báo cáo tháng - " + selectedMonth;
            byte[] file = "pdf".equalsIgnoreCase(format) ? reportFileService.exportMonthlyPdf(title, reports)
                    : "xlsx".equalsIgnoreCase(format) ? reportFileService.exportMonthlyExcel(title, reports) : null;
            return formatDownload(file, "bao-cao-thang-" + selectedMonth, format);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
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

    private void validateImportRoster(List<SessionReportImportRow> rows, List<SessionStudentReport> expectedReports,
                                      List<String> errors) {
        var expectedNames = expectedReports.stream().collect(java.util.stream.Collectors.toMap(
                r -> r.getStudent().getId(), r -> r.getStudent().getFullName()));
        Set<Long> expected = expectedNames.keySet();
        Set<Long> found = new HashSet<>();
        for (SessionReportImportRow row : rows) {
            if (row.getStudentId() == null) continue;
            if (!expected.contains(row.getStudentId())) errors.add("Dòng " + row.getSourceRow() + ": học sinh không thuộc roster của buổi học.");
            if (!found.add(row.getStudentId())) errors.add("Dòng " + row.getSourceRow() + ": mã học sinh bị lặp.");
            String expectedName = expectedNames.get(row.getStudentId());
            if (expectedName != null && row.getStudentName() != null && !row.getStudentName().isBlank()
                    && !expectedName.trim().equalsIgnoreCase(row.getStudentName().trim())) {
                errors.add("Dòng " + row.getSourceRow() + ": tên học sinh không khớp mã học sinh.");
            }
        }
        for (Long missing : expected) if (!found.contains(missing)) errors.add("Thiếu báo cáo cho mã học sinh " + missing + ".");
    }

    private ResponseEntity<byte[]> formatDownload(byte[] file, String baseName, String format) {
        if (file == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Định dạng tệp chỉ hỗ trợ xlsx hoặc pdf.");
        boolean pdf = "pdf".equalsIgnoreCase(format);
        return download(file, baseName + (pdf ? ".pdf" : ".xlsx"), pdf ? "application/pdf"
                : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    private ResponseEntity<byte[]> download(byte[] file, String filename, String contentType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(file);
    }
}
