package com.educenter.controller.admin;

import com.educenter.dto.CalendarDay;
import com.educenter.dto.ScheduleDTO;
import com.educenter.entity.ClassEntity;
import com.educenter.entity.ClassSession;
import com.educenter.entity.Schedule;
import com.educenter.repository.MonthlyStudentReportRepository;
import com.educenter.service.ClassService;
import com.educenter.service.ClassSessionService;
import com.educenter.service.RoomService;
import com.educenter.service.ScheduleService;
import com.educenter.service.StudentReportService;
import com.educenter.service.ReportFileService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Controller
@RequestMapping("/admin/schedules")
public class AdminScheduleController {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final ScheduleService scheduleService;
    private final ClassService classService;
    private final RoomService roomService;
    private final ClassSessionService classSessionService;
    private final StudentReportService studentReportService;
    private final MonthlyStudentReportRepository monthlyReportRepository;
    private final ReportFileService reportFileService;

    public AdminScheduleController(ScheduleService scheduleService, ClassService classService,
                                   RoomService roomService, ClassSessionService classSessionService,
                                   StudentReportService studentReportService,
                                   MonthlyStudentReportRepository monthlyReportRepository,
                                   ReportFileService reportFileService) {
        this.scheduleService = scheduleService;
        this.classService = classService;
        this.roomService = roomService;
        this.classSessionService = classSessionService;
        this.studentReportService = studentReportService;
        this.monthlyReportRepository = monthlyReportRepository;
        this.reportFileService = reportFileService;
    }

    @GetMapping
    public String calendar(@RequestParam(required = false) String month,
                           @RequestParam(required = false) Long classId,
                           @RequestParam(defaultValue = "false") boolean reportStatus,
                           Model model) {
        YearMonth selectedMonth = parseMonth(month);
        LocalDate first = selectedMonth.atDay(1);
        LocalDate gridStart = first.minusDays(first.getDayOfWeek().getValue() - 1L);
        List<ClassSession> sessions = classSessionService.findBetween(first, selectedMonth.atEndOfMonth(), classId);
        Map<LocalDate, List<ClassSession>> byDate = sessions.stream().collect(Collectors.groupingBy(ClassSession::getSessionDate));
        List<CalendarDay> calendarDays = new ArrayList<>();
        for (int day = 0; day < 42; day++) {
            LocalDate date = gridStart.plusDays(day);
            calendarDays.add(new CalendarDay(date, byDate.getOrDefault(date, List.of()), YearMonth.from(date).equals(selectedMonth)));
        }
        List<ClassEntity> classes = classService.getAllClasses();
        List<String> classPalette = List.of("#2563eb", "#0f9d78", "#8b5cf6", "#e77922", "#d04474", "#087f9b");
        Map<Long, String> classColors = new HashMap<>();
        for (int index = 0; index < classes.size(); index++) classColors.put(classes.get(index).getId(), classPalette.get(index % classPalette.size()));
        LocalDate today = LocalDate.now(APP_ZONE);
        java.time.LocalTime now = java.time.LocalTime.now(APP_ZONE);
        Map<Long, String> sessionVisualStates = new HashMap<>();
        for (ClassSession session : sessions) {
            boolean upcoming = "CANCELLED".equals(session.getStatus()) || session.getSessionDate().isAfter(today)
                    || (session.getSessionDate().equals(today) && session.getEndTime().isAfter(now));
            String state = reportStatus
                    ? (upcoming ? "upcoming-mode" : (session.getPublishedAt() != null ? "reported-mode" : "missed-mode"))
                    : ("CANCELLED".equals(session.getStatus()) ? "bg-secondary-subtle text-secondary" : "normal-mode");
            sessionVisualStates.put(session.getId(), state);
        }
        model.addAttribute("month", selectedMonth.toString());
        model.addAttribute("previousMonth", selectedMonth.minusMonths(1));
        model.addAttribute("nextMonth", selectedMonth.plusMonths(1));
        model.addAttribute("selectedClassId", classId);
        model.addAttribute("classes", classes);
        model.addAttribute("calendarDays", calendarDays);
        model.addAttribute("classColors", classColors);
        model.addAttribute("sessionVisualStates", sessionVisualStates);
        model.addAttribute("reportStatusView", reportStatus);
        model.addAttribute("today", today);
        model.addAttribute("now", java.time.LocalTime.now(APP_ZONE));
        model.addAttribute("classesMissingDates", classes.stream().filter(c -> c.getStartDate() == null || c.getEndDate() == null).toList());
        return "admin/schedules/calendar";
    }

    @GetMapping("/templates")
    public String scheduleTemplates(Model model) {
        model.addAttribute("schedules", scheduleService.getAllSchedules());
        return "admin/schedules/list";
    }

    @GetMapping("/monthly")
    public String monthlyReports(@RequestParam(required = false) String month,
                                 @RequestParam(required = false) Long classId, Model model) {
        YearMonth selectedMonth = parseMonth(month);
        model.addAttribute("month", selectedMonth.toString());
        model.addAttribute("classes", classService.getAllClasses());
        model.addAttribute("selectedClassId", classId);
        model.addAttribute("reports", classId == null
                ? monthlyReportRepository.findAllPublishedForMonth(selectedMonth.atDay(1))
                : monthlyReportRepository.findPublishedForClassAndMonth(classId, selectedMonth.atDay(1)));
        return "admin/schedules/monthly-reports";
    }

    @GetMapping("/monthly/export")
    public ResponseEntity<byte[]> exportMonthlyReports(@RequestParam(required = false) String month,
                                                       @RequestParam(required = false) Long classId,
                                                       @RequestParam(defaultValue = "xlsx") String format) {
        YearMonth selectedMonth = parseMonth(month);
        var reports = classId == null ? monthlyReportRepository.findAllPublishedForMonth(selectedMonth.atDay(1))
                : monthlyReportRepository.findPublishedForClassAndMonth(classId, selectedMonth.atDay(1));
        String title = "Báo cáo tháng " + selectedMonth;
        boolean pdf = "pdf".equalsIgnoreCase(format);
        byte[] bytes = pdf ? reportFileService.exportMonthlyPdf(title, reports)
                : "xlsx".equalsIgnoreCase(format) ? reportFileService.exportMonthlyExcel(title, reports) : null;
        return reportDownload(bytes, "bao-cao-thang-" + selectedMonth, format);
    }

    @GetMapping("/session/{id}")
    public String viewSession(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("classSession", classSessionService.getById(id));
            model.addAttribute("reports", studentReportService.getAdminSessionReports(id));
            model.addAttribute("rooms", roomService.findAllActive());
            return "admin/schedules/session";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/schedules";
        }
    }

    @GetMapping("/session/{id}/export")
    public ResponseEntity<byte[]> exportSessionReports(@PathVariable Long id, @RequestParam(defaultValue = "xlsx") String format) {
        ClassSession session = classSessionService.getById(id);
        var reports = studentReportService.getAdminSessionReports(id);
        String title = "Báo cáo buổi học - " + session.getClassEntity().getName() + " - " + session.getSessionDate();
        boolean pdf = "pdf".equalsIgnoreCase(format);
        byte[] bytes = pdf ? reportFileService.exportSessionPdf(title, reports)
                : "xlsx".equalsIgnoreCase(format) ? reportFileService.exportSessionExcel(title, reports) : null;
        return reportDownload(bytes, "bao-cao-buoi-" + id, format);
    }

    @PostMapping("/session/{id}/cancel")
    public String cancelSession(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            classSessionService.cancel(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy buổi học.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/schedules/session/" + id;
    }

    @PostMapping("/session/{id}/reschedule")
    public String rescheduleSession(@PathVariable Long id,
                                    @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
                                    @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime startTime,
                                    @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime endTime,
                                    @RequestParam Long roomId, RedirectAttributes redirectAttributes) {
        try {
            classSessionService.reschedule(id, date, startTime, endTime, roomId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã đổi lịch buổi học.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/schedules/session/" + id;
    }

    @GetMapping("/add")
    public String showAddForm(@RequestParam(required = false) Long classId, Model model) {
        ScheduleDTO dto = new ScheduleDTO();
        if (classId != null) dto.setClassId(classId);
        dto.setDayOfWeek(2);
        dto.setStartTime(LocalTime.of(17, 30));
        dto.setEndTime(LocalTime.of(19, 30));
        dto.setActive(true);
        model.addAttribute("scheduleDTO", dto);
        model.addAttribute("classes", classService.getActiveClasses());
        model.addAttribute("rooms", roomService.findAllActive());
        model.addAttribute("isEdit", false);
        return "admin/schedules/form";
    }

    @PostMapping("/add")
    public String addSchedule(@Valid @ModelAttribute("scheduleDTO") ScheduleDTO dto,
                              BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            fillScheduleForm(model, false);
            return "admin/schedules/form";
        }
        try {
            scheduleService.saveSchedule(dto);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xếp lịch tuần và sinh các buổi theo thời hạn lớp.");
            return "redirect:/admin/schedules";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            fillScheduleForm(model, false);
            return "admin/schedules/form";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Schedule s = scheduleService.getScheduleById(id);
            ScheduleDTO dto = new ScheduleDTO();
            dto.setId(s.getId());
            dto.setClassId(s.getClassEntity().getId());
            dto.setRoomId(s.getRoom().getId());
            dto.setDayOfWeek(s.getDayOfWeek());
            dto.setStartTime(s.getStartTime());
            dto.setEndTime(s.getEndTime());
            dto.setActive(s.getActive());
            model.addAttribute("scheduleDTO", dto);
            fillScheduleForm(model, true);
            return "admin/schedules/form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/schedules";
        }
    }

    @PostMapping("/edit/{id}")
    public String updateSchedule(@PathVariable Long id, @Valid @ModelAttribute("scheduleDTO") ScheduleDTO dto,
                                 BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            fillScheduleForm(model, true);
            return "admin/schedules/form";
        }
        try {
            scheduleService.updateSchedule(id, dto);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật lịch tuần; các buổi tương lai chưa có báo cáo được đồng bộ.");
            return "redirect:/admin/schedules";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            fillScheduleForm(model, true);
            return "admin/schedules/form";
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteSchedule(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            scheduleService.deleteSchedule(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa mẫu lịch tuần; lịch sử buổi đã qua được giữ lại.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/schedules";
    }

    private void fillScheduleForm(Model model, boolean edit) {
        model.addAttribute("classes", classService.getActiveClasses());
        model.addAttribute("rooms", roomService.findAllActive());
        model.addAttribute("isEdit", edit);
    }

    private YearMonth parseMonth(String value) {
        try {
            return value == null || value.isBlank() ? YearMonth.now(APP_ZONE) : YearMonth.parse(value);
        } catch (RuntimeException e) {
            return YearMonth.now(APP_ZONE);
        }
    }

    private ResponseEntity<byte[]> reportDownload(byte[] bytes, String baseName, String format) {
        if (bytes == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Định dạng tệp chỉ hỗ trợ xlsx hoặc pdf.");
        boolean pdf = "pdf".equalsIgnoreCase(format);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(pdf ? "application/pdf" : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(baseName + (pdf ? ".pdf" : ".xlsx"), StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(bytes);
    }
}
