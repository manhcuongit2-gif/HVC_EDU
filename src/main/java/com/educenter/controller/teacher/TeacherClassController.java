package com.educenter.controller.teacher;

import com.educenter.entity.*;
import com.educenter.repository.ClassRepository;
import com.educenter.repository.TeacherRepository;
import com.educenter.security.CustomUserDetails;
import com.educenter.service.AttendanceService;
import com.educenter.service.ClassService;
import com.educenter.service.GradeService;
import com.educenter.service.ScheduleService;
import com.educenter.util.VietnameseDateFormatter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    public TeacherClassController(ClassRepository classRepository,
                                  TeacherRepository teacherRepository,
                                  ClassService classService,
                                  AttendanceService attendanceService,
                                  GradeService gradeService,
                                  ScheduleService scheduleService) {
        this.classRepository = classRepository;
        this.teacherRepository = teacherRepository;
        this.classService = classService;
        this.attendanceService = attendanceService;
        this.gradeService = gradeService;
        this.scheduleService = scheduleService;
    }

    @GetMapping("/classes")
    public String myClasses(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        List<ClassEntity> classes = classRepository.findByTeacherUserId(userDetails.getId());
        Map<Long, Long> studentCounts = new HashMap<>();
        for (ClassEntity c : classes) {
            studentCounts.put(c.getId(), classService.getStudentCountInClass(c.getId()));
        }

        model.addAttribute("classes", classes);
        model.addAttribute("studentCounts", studentCounts);
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
    public String mySchedules(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        List<Schedule> schedules = scheduleService.getSchedulesByTeacherUserId(userDetails.getId());
        model.addAttribute("schedules", schedules);
        model.addAttribute("currentUser", userDetails);
        return "teacher/schedules";
    }
}
