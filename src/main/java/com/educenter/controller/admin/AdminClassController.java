package com.educenter.controller.admin;

import com.educenter.dto.ClassDTO;
import com.educenter.entity.ClassEntity;
import com.educenter.entity.ClassStudent;
import com.educenter.security.CustomUserDetails;
import com.educenter.service.ClassService;
import com.educenter.service.StudentService;
import com.educenter.service.SubjectService;
import com.educenter.service.TeacherService;
import com.educenter.service.TeacherPayService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequestMapping("/admin/classes")
public class AdminClassController {

    private final ClassService classService;
    private final SubjectService subjectService;
    private final TeacherService teacherService;
    private final StudentService studentService;
    private final TeacherPayService teacherPayService;

    public AdminClassController(ClassService classService,
                                SubjectService subjectService,
                                TeacherService teacherService,
                                StudentService studentService,
                                TeacherPayService teacherPayService) {
        this.classService = classService;
        this.subjectService = subjectService;
        this.teacherService = teacherService;
        this.studentService = studentService;
        this.teacherPayService = teacherPayService;
    }

    @GetMapping
    public String listClasses(Model model) {
        List<ClassEntity> classes = classService.getAllClasses();
        Map<Long, Long> studentCounts = new HashMap<>();
        for (ClassEntity c : classes) {
            studentCounts.put(c.getId(), classService.getStudentCountInClass(c.getId()));
        }

        model.addAttribute("classes", classes);
        model.addAttribute("studentCounts", studentCounts);
        return "admin/classes/list";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        ClassDTO classDTO = new ClassDTO();
        classDTO.setStatus("ACTIVE");
        classDTO.setMaxStudents(30);

        model.addAttribute("classDTO", classDTO);
        model.addAttribute("subjects", subjectService.findAllActive());
        model.addAttribute("teachers", teacherService.findAllActive());
        model.addAttribute("isEdit", false);
        return "admin/classes/form";
    }

    @PostMapping("/add")
    public String addClass(@Valid @ModelAttribute("classDTO") ClassDTO classDTO,
                           BindingResult bindingResult,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (classService.isClassNameTaken(classDTO.getName(), null)) {
            bindingResult.rejectValue("name", "error.name", "Tên lớp học này đã tồn tại");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("subjects", subjectService.findAllActive());
            model.addAttribute("teachers", teacherService.findAllActive());
            model.addAttribute("isEdit", false);
            return "admin/classes/form";
        }

        try {
            classService.saveClass(classDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm mới lớp học thành công!");
            return "redirect:/admin/classes";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("subjects", subjectService.findAllActive());
            model.addAttribute("teachers", teacherService.findAllActive());
            model.addAttribute("isEdit", false);
            return "admin/classes/form";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            ClassEntity c = classService.getClassById(id);
            ClassDTO classDTO = new ClassDTO();
            classDTO.setId(c.getId());
            classDTO.setName(c.getName());
            classDTO.setSubjectId(c.getSubject().getId());
            classDTO.setTeacherId(c.getTeacher().getId());
            classDTO.setAssistantTeacherId(c.getAssistantTeacher() == null ? null : c.getAssistantTeacher().getId());
            classDTO.setMaxStudents(c.getMaxStudents());
            classDTO.setTuitionFee(c.getTuitionFee());
            classDTO.setSessionFee(c.getSessionFee());
            classDTO.setStatus(c.getStatus());
            classDTO.setStartDate(c.getStartDate());
            classDTO.setEndDate(c.getEndDate());
            classDTO.setDescription(c.getDescription());

            model.addAttribute("classDTO", classDTO);
            model.addAttribute("subjects", subjectService.findAllActive());
            model.addAttribute("teachers", teacherService.findAllActive());
            model.addAttribute("isEdit", true);
            return "admin/classes/form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/classes";
        }
    }

    @PostMapping("/edit/{id}")
    public String updateClass(@PathVariable Long id,
                              @Valid @ModelAttribute("classDTO") ClassDTO classDTO,
                              BindingResult bindingResult,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (classService.isClassNameTaken(classDTO.getName(), id)) {
            bindingResult.rejectValue("name", "error.name", "Tên lớp học này đã tồn tại");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("subjects", subjectService.findAllActive());
            model.addAttribute("teachers", teacherService.findAllActive());
            model.addAttribute("isEdit", true);
            return "admin/classes/form";
        }

        try {
            classService.updateClass(id, classDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin lớp học thành công!");
            return "redirect:/admin/classes";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("subjects", subjectService.findAllActive());
            model.addAttribute("teachers", teacherService.findAllActive());
            model.addAttribute("isEdit", true);
            return "admin/classes/form";
        }
    }

    @GetMapping("/{id}")
    public String viewClassDetail(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            ClassEntity classEntity = classService.getClassById(id);
            List<ClassStudent> enrolledStudents = classService.getStudentsInClass(id);
            long studentCount = enrolledStudents.size();

            model.addAttribute("classEntity", classEntity);
            model.addAttribute("enrolledStudents", enrolledStudents);
            model.addAttribute("studentCount", studentCount);
            model.addAttribute("allStudents", studentService.findAllActive());
            model.addAttribute("payRates", teacherPayService.getRateHistory(id));
            model.addAttribute("payRateToday", LocalDate.now());
            return "admin/classes/detail";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/classes";
        }
    }

    @PostMapping("/{id}/add-student")
    public String addStudentToClass(@PathVariable Long id,
                                   @RequestParam Long studentId,
                                   RedirectAttributes redirectAttributes) {
        try {
            classService.addStudentToClass(id, studentId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm học sinh vào lớp học thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/classes/" + id;
    }

    @PostMapping("/{id}/remove-student/{studentId}")
    public String removeStudentFromClass(@PathVariable Long id,
                                        @PathVariable Long studentId,
                                        RedirectAttributes redirectAttributes) {
        try {
            classService.removeStudentFromClass(id, studentId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa học sinh khỏi lớp học!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/classes/" + id;
    }

    @PostMapping("/{id}/pay-rates")
    public String addTeacherPayRate(@PathVariable Long id,
                                    @RequestParam BigDecimal rate,
                                    @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate effectiveFrom,
                                    @AuthenticationPrincipal CustomUserDetails user,
                                    RedirectAttributes redirectAttributes) {
        try {
            teacherPayService.recordRate(user.getId(), id, rate, effectiveFrom);
            redirectAttributes.addFlashAttribute("successMessage", "Đã lưu đơn giá dạy cho lớp.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/classes/" + id;
    }

    @PostMapping("/delete/{id}")
    public String deleteClass(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            classService.deleteClass(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa lớp học thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa lớp học: " + e.getMessage());
        }
        return "redirect:/admin/classes";
    }
}
