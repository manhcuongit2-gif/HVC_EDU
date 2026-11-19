package com.educenter.controller.teacher;

import com.educenter.entity.*;
import com.educenter.repository.ClassRepository;
import com.educenter.repository.TeacherRepository;
import com.educenter.security.CustomUserDetails;
import com.educenter.service.AttendanceService;
import com.educenter.service.ClassService;
import com.educenter.service.GradeService;
import com.educenter.service.ScheduleService;
import com.educenter.service.ClassSessionService;
import com.educenter.util.VietnameseDateFormatter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/teacher")
public class TeacherClassController {

    private final ClassRepository classRepository;
    private final TeacherRepository teacherRepository;
    private final ClassService classService;
    private final AttendanceService attendanceService;
    private final GradeService gradeService;
    private final ScheduleService scheduleService;
    private final ClassSessionService classSessionService;

    public TeacherClassController(ClassRepository classRepository,
                                  TeacherRepository teacherRepository,
                                  ClassService classService,
                                  AttendanceService attendanceService,
                                  GradeService gradeService,
                                  ScheduleService scheduleService,
                                  ClassSessionService classSessionService) {
        this.classRepository = classRepository;
        this.teacherRepository = teacherRepository;
        this.classService = classService;
        this.attendanceService = attendanceService;
        this.gradeService = gradeService;
        this.scheduleService = scheduleService;
        this.classSessionService = classSessionService;
    }

    @GetMapping("/classes")
    public String myClasses(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        List<ClassEntity> classes = classRepository.findByTeacherUserId(userDetails.getId());
        Map<Long, Long> studentCounts = new HashMap<>();
        Map<Long, List<Schedule>> schedulesByClassId = new HashMap<>();
        for (ClassEntity c : classes) {
            studentCounts.put(c.getId(), classService.getStudentCountInClass(c.getId()));
            schedulesByClassId.put(c.getId(), scheduleService.getSchedulesByClassId(c.getId()));
        }

        model.addAttribute("classes", classes);
        model.addAttribute("studentCounts", studentCounts);
        model.addAttribute("schedulesByClassId", schedulesByClassId);
        model.addAttribute("currentUser", userDetails);
        model.addAttribute("canManageStudents", teacherRepository.findByUserId(userDetails.getId())
                .map(Teacher::isCanManageStudents).orElse(false));
        return "teacher/classes";
    }

    @GetMapping("/attendance/{classId}")
    public String showAttendance(@PathVariable Long classId,
                                 @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
                                 @AuthenticationPrincipal CustomUserDetails userDetails,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        try {
            ClassEntity classEntity = classService.getClassById(classId);
            LocalDate targetDate = (date != null) ? date : LocalDate.now();

            List<ClassStudent> enrolledStudents = classService.getStudentsInClass(classId);
            List<Attendance> attendanceRecords = attendanceService.getAttendanceByClassAndDate(classId, targetDate);

            Map<Long, Attendance> attendanceMap = new HashMap<>();
            for (Attendance a : attendanceRecords) {
                attendanceMap.put(a.getStudent().getId(), a);
            }

            model.addAttribute("classEntity", classEntity);
            model.addAttribute("enrolledStudents", enrolledStudents);
            model.addAttribute("targetDate", targetDate);
            model.addAttribute("targetWeekdayDisplay", VietnameseDateFormatter.weekday(targetDate));
            model.addAttribute("attendanceMap", attendanceMap);
            model.addAttribute("currentUser", userDetails);
            return "teacher/attendance";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/teacher/classes";
        }
    }

    @PostMapping("/attendance/{classId}")
    public String saveAttendance(@PathVariable Long classId,
                                 @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
                                 @RequestParam List<Long> studentIds,
                                 @RequestParam List<String> statuses,
                                 @RequestParam(required = false) List<String> notes,
                                 @AuthenticationPrincipal CustomUserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        try {
            attendanceService.saveAttendanceBatch(classId, date, studentIds, statuses, notes, userDetails.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Lưu điểm danh ngày " + date + " thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/teacher/attendance/" + classId + "?date=" + date;
    }

    @GetMapping("/grades/{classId}")
    public String showGrades(@PathVariable Long classId,
                             @AuthenticationPrincipal CustomUserDetails userDetails,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        try {
            ClassEntity classEntity = classService.getClassById(classId);
            List<ClassStudent> enrolledStudents = classService.getStudentsInClass(classId);
            List<Grade> gradeRecords = gradeService.getGradesByClassId(classId);

            Map<Long, Grade> gradeMap = new HashMap<>();
            for (Grade g : gradeRecords) {
                gradeMap.put(g.getStudent().getId(), g);
            }

            model.addAttribute("classEntity", classEntity);
            model.addAttribute("enrolledStudents", enrolledStudents);
            model.addAttribute("gradeMap", gradeMap);
            model.addAttribute("currentUser", userDetails);
            return "teacher/grades";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/teacher/classes";
        }
    }

    @PostMapping("/grades/{classId}")
    public String saveGrades(@PathVariable Long classId,
                             @RequestParam List<Long> studentIds,
                             @RequestParam(required = false) List<BigDecimal> testScores,
                             @RequestParam(required = false) List<BigDecimal> midtermScores,
                             @RequestParam(required = false) List<BigDecimal> finalScores,
                             @RequestParam(required = false) List<String> notes,
                             RedirectAttributes redirectAttributes) {
        try {
            gradeService.saveGradeBatch(classId, studentIds, testScores, midtermScores, finalScores, notes);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật bảng điểm lớp học thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/teacher/grades/" + classId;
    }

    @GetMapping("/schedules")
    public String mySchedules(@AuthenticationPrincipal CustomUserDetails userDetails,
                              @RequestParam(required = false) String view,
                              @RequestParam(required = false) String date,
                              Model model) {
        String selectedView = "month".equalsIgnoreCase(view) ? "month" : "week";
        LocalDate anchor = parseScheduleDate(date);
        LocalDate from = selectedView.equals("month")
                ? anchor.withDayOfMonth(1)
                : anchor.with(DayOfWeek.MONDAY);
        LocalDate to = selectedView.equals("month")
                ? from.plusMonths(1).minusDays(1)
                : from.plusDays(6);
        var sessions = classSessionService.findForTeacher(userDetails.getId(), from, to);
        Map<Long, Long> studentCounts = new HashMap<>();
        for (ClassSession session : sessions) {
            studentCounts.computeIfAbsent(session.getClassEntity().getId(), classService::getStudentCountInClass);
        }
        LocalDate step = selectedView.equals("month") ? from.plusMonths(1) : from.plusWeeks(1);
        LocalDate previous = selectedView.equals("month") ? from.minusMonths(1) : from.minusWeeks(1);
        model.addAttribute("sessions", sessions);
        model.addAttribute("studentCounts", studentCounts);
        model.addAttribute("view", selectedView);
        model.addAttribute("selectedDate", anchor);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("previousDate", previous);
        model.addAttribute("nextDate", step);
        model.addAttribute("today", LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")));
        model.addAttribute("currentUser", userDetails);
        return "teacher/schedules";
    }

    private LocalDate parseScheduleDate(String value) {
        if (value == null) return LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
        try { return LocalDate.parse(value); }
        catch (DateTimeParseException ignored) { return LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")); }
    }
}
