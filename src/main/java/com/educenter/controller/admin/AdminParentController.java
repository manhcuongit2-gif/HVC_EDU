package com.educenter.controller.admin;

import com.educenter.dto.ParentDTO;
import com.educenter.entity.Parent;
import com.educenter.security.CustomUserDetails;
import com.educenter.service.ParentService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/parents")
public class AdminParentController {

    private final ParentService parentService;

    public AdminParentController(ParentService parentService) {
        this.parentService = parentService;
    }

    // Danh sách phụ huynh
    @GetMapping
    public String list(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("currentUser", userDetails);
        model.addAttribute("parents", parentService.findAll());
        return "admin/parents/list";
    }

    // Form thêm mới phụ huynh
    @GetMapping("/add")
    public String showAddForm(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("currentUser", userDetails);
        model.addAttribute("parentDTO", new ParentDTO());
        return "admin/parents/form";
    }

    // Xử lý thêm mới phụ huynh
    @PostMapping("/add")
    public String addParent(@Valid @ModelAttribute("parentDTO") ParentDTO dto,
                            BindingResult result,
                            @AuthenticationPrincipal CustomUserDetails userDetails,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        if (parentService.existsByUsername(dto.getUsername())) {
            result.rejectValue("username", "error.parentDTO", "Tên đăng nhập đã được sử dụng");
        }

        if (parentService.existsByEmail(dto.getEmail())) {
            result.rejectValue("email", "error.parentDTO", "Email này đã được đăng ký trong hệ thống");
        }

        if (result.hasErrors()) {
            model.addAttribute("currentUser", userDetails);
            return "admin/parents/form";
        }

        parentService.createParent(dto);
        redirectAttributes.addFlashAttribute("successMessage", "Thêm phụ huynh \"" + dto.getFullName() + "\" thành công!");
        return "redirect:/admin/parents";
    }

    // Form sửa thông tin phụ huynh
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id,
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        Parent parent = parentService.findById(id).orElse(null);
        if (parent == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy phụ huynh với ID: " + id);
            return "redirect:/admin/parents";
        }

        ParentDTO dto = new ParentDTO();
        dto.setId(parent.getId());
        dto.setUsername(parent.getUser().getUsername());
        dto.setFullName(parent.getUser().getFullName());
        dto.setEmail(parent.getUser().getEmail());
        dto.setPhone(parent.getUser().getPhone());
        dto.setAddress(parent.getUser().getAddress());
        dto.setOccupation(parent.getOccupation());
        dto.setNote(parent.getNote());
        dto.setActive(parent.getUser().getActive());

        model.addAttribute("currentUser", userDetails);
        model.addAttribute("parentDTO", dto);
        return "admin/parents/form";
    }

    // Xử lý cập nhật thông tin phụ huynh
    @PostMapping("/edit/{id}")
    public String updateParent(@PathVariable Long id,
                               @Valid @ModelAttribute("parentDTO") ParentDTO dto,
                               BindingResult result,
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        Parent existing = parentService.findById(id).orElse(null);
        if (existing == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy phụ huynh!");
            return "redirect:/admin/parents";
        }

        if (!existing.getUser().getEmail().equals(dto.getEmail()) && parentService.existsByEmail(dto.getEmail())) {
            result.rejectValue("email", "error.parentDTO", "Email này đã được sử dụng");
        }

        if (result.hasErrors()) {
            model.addAttribute("currentUser", userDetails);
            dto.setId(id);
            dto.setUsername(existing.getUser().getUsername());
            return "admin/parents/form";
        }

        parentService.updateParent(id, dto);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin phụ huynh \"" + dto.getFullName() + "\" thành công!");
        return "redirect:/admin/parents";
    }

    // Bật/Tắt trạng thái hoạt động tài khoản
    @GetMapping("/toggle/{id}")
    public String toggleActive(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Parent parent = parentService.findById(id).orElse(null);
        if (parent == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy phụ huynh!");
            return "redirect:/admin/parents";
        }
        parentService.toggleActive(id);
        String status = parent.getUser().getActive() ? "Đã khóa" : "Đã kích hoạt";
        redirectAttributes.addFlashAttribute("successMessage", status + " tài khoản phụ huynh \"" + parent.getUser().getFullName() + "\"!");
        return "redirect:/admin/parents";
    }
}
