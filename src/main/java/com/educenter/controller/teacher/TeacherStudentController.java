package com.educenter.controller.teacher;

import com.educenter.dto.TeacherStudentForm;
import com.educenter.entity.ClassEntity;
import com.educenter.entity.Student;
import com.educenter.security.CustomUserDetails;
import com.educenter.service.ParentService;
import com.educenter.service.TeacherStudentService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/teacher/classes/{classId}/students")
public class TeacherStudentController {

    private final TeacherStudentService teacherStudentService;
    private final ParentService parentService;

    public TeacherStudentController(TeacherStudentService teacherStudentService, ParentService parentService) {
        this.teacherStudentService = teacherStudentService;
        this.parentService = parentService;
    }

    @GetMapping
    public String roster(@PathVariable Long classId, @AuthenticationPrincipal CustomUserDetails user, Model model) {
        try {
            populateRoster(classId, user, model);
            return "teacher/class-students";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("classes", java.util.List.of());
            return "error/403";
        }
    }

    @PostMapping
    public String addExisting(@PathVariable Long classId, @RequestParam Long studentId,
                              @AuthenticationPrincipal CustomUserDetails user,
                              RedirectAttributes redirectAttributes) {
        try {
            teacherStudentService.addExisting(user.getId(), classId, studentId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm học sinh vào lớp.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/teacher/classes/" + classId + "/students";
    }

    @PostMapping("/new")
    public String create(@PathVariable Long classId,
                         @Valid @ModelAttribute("studentForm") TeacherStudentForm form,
                         BindingResult result,
                         @AuthenticationPrincipal CustomUserDetails user,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            populateRoster(classId, user, model);
            model.addAttribute("studentForm", form);
            model.addAttribute("errorMessage", result.getFieldErrors().getFirst().getDefaultMessage());
            return "teacher/class-students";
        }
        try {
            teacherStudentService.createAndEnroll(user.getId(), classId, form);
            redirectAttributes.addFlashAttribute("successMessage", "Đã tạo hồ sơ và thêm học sinh vào lớp.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/teacher/classes/" + classId + "/students";
    }

    @GetMapping("/{studentId}/edit")
    public String editForm(@PathVariable Long classId, @PathVariable Long studentId,
                           @AuthenticationPrincipal CustomUserDetails user, Model model,
                           RedirectAttributes redirectAttributes) {
        try {
            ClassEntity classEntity = teacherStudentService.getClassForTeacher(user.getId(), classId);
            Student student = teacherStudentService.getStudentForEdit(user.getId(), classId, studentId);
            model.addAttribute("classEntity", classEntity);
            model.addAttribute("student", student);
            model.addAttribute("studentForm", toForm(student));
            model.addAttribute("currentUser", user);
            return "teacher/student-form";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/teacher/classes/" + classId + "/students";
        }
    }

    @PostMapping("/{studentId}/edit")
    public String update(@PathVariable Long classId, @PathVariable Long studentId,
                         @Valid @ModelAttribute("studentForm") TeacherStudentForm form,
                         BindingResult result,
                         @AuthenticationPrincipal CustomUserDetails user,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            try {
                model.addAttribute("classEntity", teacherStudentService.getClassForTeacher(user.getId(), classId));
                model.addAttribute("student", teacherStudentService.getStudentForEdit(user.getId(), classId, studentId));
                model.addAttribute("studentForm", form);
                model.addAttribute("currentUser", user);
                model.addAttribute("errorMessage", result.getFieldErrors().getFirst().getDefaultMessage());
                return "teacher/student-form";
            } catch (IllegalArgumentException e) {
                redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
                return "redirect:/teacher/classes/" + classId + "/students";
            }
        }
        try {
            teacherStudentService.updateStudent(user.getId(), classId, studentId, form);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật hồ sơ học sinh.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/teacher/classes/" + classId + "/students";
    }

    @PostMapping("/{studentId}/drop")
    public String drop(@PathVariable Long classId, @PathVariable Long studentId,
                       @AuthenticationPrincipal CustomUserDetails user, RedirectAttributes redirectAttributes) {
        try {
            teacherStudentService.dropFromClass(user.getId(), classId, studentId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã gỡ học sinh khỏi lớp. Lịch sử học tập được giữ lại.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/teacher/classes/" + classId + "/students";
    }

    private void populateRoster(Long classId, CustomUserDetails user, Model model) {
        ClassEntity classEntity = teacherStudentService.getClassForTeacher(user.getId(), classId);
        model.addAttribute("classEntity", classEntity);
        model.addAttribute("enrolledStudents", teacherStudentService.getRoster(user.getId(), classId));
        model.addAttribute("availableStudents", teacherStudentService.getAvailableStudents(user.getId(), classId));
        model.addAttribute("parents", parentService.findAllActive());
        model.addAttribute("studentForm", new TeacherStudentForm());
        model.addAttribute("currentUser", user);
    }

    private TeacherStudentForm toForm(Student student) {
        TeacherStudentForm form = new TeacherStudentForm();
        form.setFullName(student.getFullName());
        form.setDateOfBirth(student.getDateOfBirth());
        form.setGender(student.getGender());
        form.setPhone(student.getPhone());
        form.setSchool(student.getSchool());
        form.setGradeLevel(student.getGradeLevel());
        return form;
    }
}
