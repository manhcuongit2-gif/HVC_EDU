package com.educenter.controller.admin;

import com.educenter.dto.StudentDTO;
import com.educenter.entity.Student;
import com.educenter.security.CustomUserDetails;
import com.educenter.service.ParentService;
import com.educenter.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/students")
public class AdminStudentController {

    private final StudentService studentService;
    private final ParentService parentService;

    public AdminStudentController(StudentService studentService, ParentService parentService) {
        this.studentService = studentService;
        this.parentService = parentService;
    }

    // Danh sách học sinh
    @GetMapping
    public String list(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("currentUser", userDetails);
        model.addAttribute("students", studentService.findAll());
        return "admin/students/list";
    }

    // Form thêm mới học sinh
    @GetMapping("/add")
    public String showAddForm(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("currentUser", userDetails);
        model.addAttribute("studentDTO", new StudentDTO());
        model.addAttribute("parents", parentService.findAllActive());
        return "admin/students/form";
    }

    // Xử lý thêm mới học sinh
    @PostMapping("/add")
    public String addStudent(@Valid @ModelAttribute("studentDTO") StudentDTO dto,
                             BindingResult result,
                             @AuthenticationPrincipal CustomUserDetails userDetails,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("currentUser", userDetails);
            model.addAttribute("parents", parentService.findAllActive());
            return "admin/students/form";
        }

        studentService.createStudent(dto);
        redirectAttributes.addFlashAttribute("successMessage", "Thêm học sinh \"" + dto.getFullName() + "\" thành công!");
        return "redirect:/admin/students";
    }

    // Form sửa thông tin học sinh
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id,
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        Student student = studentService.findById(id).orElse(null);
        if (student == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy học sinh với ID: " + id);
            return "redirect:/admin/students";
        }

        StudentDTO dto = new StudentDTO();
        dto.setId(student.getId());
        dto.setFullName(student.getFullName());
        dto.setDateOfBirth(student.getDateOfBirth());
        dto.setGender(student.getGender());
        dto.setPhone(student.getPhone());
        dto.setSchool(student.getSchool());
        dto.setGradeLevel(student.getGradeLevel());
        dto.setParentId(student.getParent().getId());
        dto.setActive(student.getActive());

        model.addAttribute("currentUser", userDetails);
        model.addAttribute("studentDTO", dto);
        model.addAttribute("parents", parentService.findAllActive());
        return "admin/students/form";
    }

    // Xử lý cập nhật thông tin học sinh
    @PostMapping("/edit/{id}")
    public String updateStudent(@PathVariable Long id,
                                @Valid @ModelAttribute("studentDTO") StudentDTO dto,
                                BindingResult result,
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("currentUser", userDetails);
            dto.setId(id);
            model.addAttribute("parents", parentService.findAllActive());
            return "admin/students/form";
        }

        studentService.updateStudent(id, dto);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật học sinh \"" + dto.getFullName() + "\" thành công!");
        return "redirect:/admin/students";
    }

    // Bật/Tắt trạng thái hoạt động
    @GetMapping("/toggle/{id}")
    public String toggleActive(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Student student = studentService.findById(id).orElse(null);
        if (student == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy học sinh!");
            return "redirect:/admin/students";
        }
        studentService.toggleActive(id);
        String status = student.getActive() ? "Đã khóa" : "Đã kích hoạt";
        redirectAttributes.addFlashAttribute("successMessage", status + " hồ sơ học sinh \"" + student.getFullName() + "\"!");
        return "redirect:/admin/students";
    }
}
