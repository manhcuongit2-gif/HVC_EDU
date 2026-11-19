package com.educenter.controller.admin;

import com.educenter.dto.TeacherDTO;
import com.educenter.entity.Teacher;
import com.educenter.security.CustomUserDetails;
import com.educenter.service.TeacherService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/teachers")
public class AdminTeacherController {

    private final TeacherService teacherService;

    public AdminTeacherController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    // Danh sách người dạy
    @GetMapping
    public String list(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("currentUser", userDetails);
        model.addAttribute("teachers", teacherService.findAll());
        return "admin/teachers/list";
    }

    // Form thêm mới người dạy
    @GetMapping("/add")
    public String showAddForm(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("currentUser", userDetails);
        model.addAttribute("teacherDTO", new TeacherDTO());
        return "admin/teachers/form";
    }

    // Xử lý thêm mới người dạy
    @PostMapping("/add")
    public String addTeacher(@Valid @ModelAttribute("teacherDTO") TeacherDTO dto,
                             BindingResult result,
                             @AuthenticationPrincipal CustomUserDetails userDetails,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        // Kiểm tra username tồn tại
        if (teacherService.existsByUsername(dto.getUsername())) {
            result.rejectValue("username", "error.teacherDTO", "Tên đăng nhập đã được sử dụng");
        }

        // Kiểm tra email tồn tại
        if (teacherService.existsByEmail(dto.getEmail())) {
            result.rejectValue("email", "error.teacherDTO", "Email này đã được đăng ký trong hệ thống");
        }

        if (result.hasErrors()) {
            model.addAttribute("currentUser", userDetails);
            return "admin/teachers/form";
        }

        teacherService.createTeacher(dto);
        redirectAttributes.addFlashAttribute("successMessage", "Thêm người dạy \"" + dto.getFullName() + "\" thành công!");
        return "redirect:/admin/teachers";
    }

    // Form sửa thông tin người dạy
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id,
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        Teacher teacher = teacherService.findById(id).orElse(null);
        if (teacher == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy người dạy với ID: " + id);
            return "redirect:/admin/teachers";
        }

        TeacherDTO dto = new TeacherDTO();
        dto.setId(teacher.getId());
        dto.setUsername(teacher.getUser().getUsername());
        dto.setFullName(teacher.getUser().getFullName());
        dto.setEmail(teacher.getUser().getEmail());
        dto.setPhone(teacher.getUser().getPhone());
        dto.setAddress(teacher.getUser().getAddress());
        dto.setSpecialization(teacher.getSpecialization());
        dto.setDegree(teacher.getDegree());
        dto.setExperience(teacher.getExperience());
        dto.setActive(teacher.getUser().getActive());

        model.addAttribute("currentUser", userDetails);
        model.addAttribute("teacherDTO", dto);
        return "admin/teachers/form";
    }

    // Xử lý cập nhật thông tin người dạy
    @PostMapping("/edit/{id}")
    public String updateTeacher(@PathVariable Long id,
                                @Valid @ModelAttribute("teacherDTO") TeacherDTO dto,
                                BindingResult result,
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        Teacher existing = teacherService.findById(id).orElse(null);
        if (existing == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy người dạy!");
            return "redirect:/admin/teachers";
        }

        // Kiểm tra trùng email nếu email thay đổi
        if (!existing.getUser().getEmail().equals(dto.getEmail()) && teacherService.existsByEmail(dto.getEmail())) {
            result.rejectValue("email", "error.teacherDTO", "Email này đã được sử dụng");
        }

        if (result.hasErrors()) {
            model.addAttribute("currentUser", userDetails);
            dto.setId(id);
            dto.setUsername(existing.getUser().getUsername()); // Không cho sửa username
            return "admin/teachers/form";
        }

        teacherService.updateTeacher(id, dto);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin người dạy \"" + dto.getFullName() + "\" thành công!");
        return "redirect:/admin/teachers";
    }

    // Bật/Tắt trạng thái hoạt động tài khoản
    @GetMapping("/toggle/{id}")
    public String toggleActive(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Teacher teacher = teacherService.findById(id).orElse(null);
        if (teacher == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy người dạy!");
            return "redirect:/admin/teachers";
        }
        teacherService.toggleActive(id);
        String status = teacher.getUser().getActive() ? "Đã khóa" : "Đã kích hoạt";
        redirectAttributes.addFlashAttribute("successMessage", status + " tài khoản người dạy \"" + teacher.getUser().getFullName() + "\"!");
        return "redirect:/admin/teachers";
    }

    @PostMapping("/{id}/student-management")
    public String setStudentManagement(@PathVariable Long id, @RequestParam boolean enabled,
                                       RedirectAttributes redirectAttributes) {
        try {
            teacherService.setStudentManagementPermission(id, enabled);
            redirectAttributes.addFlashAttribute("successMessage", enabled
                    ? "Đã cấp quyền quản lý danh sách học sinh cho người dạy."
                    : "Đã thu hồi quyền quản lý danh sách học sinh của người dạy.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/teachers";
    }
}
