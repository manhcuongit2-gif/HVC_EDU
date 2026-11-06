package com.educenter.controller.admin;

import com.educenter.entity.Subject;
import com.educenter.security.CustomUserDetails;
import com.educenter.service.SubjectService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/subjects")
public class AdminSubjectController {

    private final SubjectService subjectService;

    public AdminSubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    // Danh sách môn học
    @GetMapping
    public String list(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("currentUser", userDetails);
        model.addAttribute("subjects", subjectService.findAll());
        return "admin/subjects/list";
    }

    // Form thêm mới môn học
    @GetMapping("/add")
    public String showAddForm(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("currentUser", userDetails);
        model.addAttribute("subject", new Subject());
        return "admin/subjects/form";
    }

    // Xử lý thêm mới môn học
    @PostMapping("/add")
    public String addSubject(@Valid @ModelAttribute("subject") Subject subject,
                             BindingResult result,
                             @AuthenticationPrincipal CustomUserDetails userDetails,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        // Kiểm tra trùng tên
        if (subjectService.existsByName(subject.getName())) {
            result.rejectValue("name", "error.subject", "Tên môn học đã tồn tại trong hệ thống");
        }

        if (result.hasErrors()) {
            model.addAttribute("currentUser", userDetails);
            return "admin/subjects/form";
        }

        subject.setActive(true);
        subjectService.save(subject);
        redirectAttributes.addFlashAttribute("successMessage", "Thêm môn học \"" + subject.getName() + "\" thành công!");
        return "redirect:/admin/subjects";
    }

    // Form sửa môn học
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id,
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        Subject subject = subjectService.findById(id).orElse(null);
        if (subject == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy môn học với ID: " + id);
            return "redirect:/admin/subjects";
        }
        model.addAttribute("currentUser", userDetails);
        model.addAttribute("subject", subject);
        return "admin/subjects/form";
    }

    // Xử lý cập nhật môn học
    @PostMapping("/edit/{id}")
    public String updateSubject(@PathVariable Long id,
                                @Valid @ModelAttribute("subject") Subject subject,
                                BindingResult result,
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        Subject existing = subjectService.findById(id).orElse(null);
        if (existing == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy môn học!");
            return "redirect:/admin/subjects";
        }

        // Kiểm tra trùng tên (loại trừ chính nó)
        if (!existing.getName().equals(subject.getName()) && subjectService.existsByName(subject.getName())) {
            result.rejectValue("name", "error.subject", "Tên môn học đã tồn tại trong hệ thống");
        }

        if (result.hasErrors()) {
            model.addAttribute("currentUser", userDetails);
            subject.setId(id);
            return "admin/subjects/form";
        }

        existing.setName(subject.getName());
        existing.setDescription(subject.getDescription());
        subjectService.save(existing);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật môn học \"" + existing.getName() + "\" thành công!");
        return "redirect:/admin/subjects";
    }

    // Bật/Tắt trạng thái hoạt động
    @GetMapping("/toggle/{id}")
    public String toggleActive(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Subject subject = subjectService.findById(id).orElse(null);
        if (subject == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy môn học!");
            return "redirect:/admin/subjects";
        }
        subjectService.toggleActive(id);
        String status = subject.getActive() ? "Đã khóa" : "Đã kích hoạt";
        redirectAttributes.addFlashAttribute("successMessage", status + " môn học \"" + subject.getName() + "\"!");
        return "redirect:/admin/subjects";
    }
}
