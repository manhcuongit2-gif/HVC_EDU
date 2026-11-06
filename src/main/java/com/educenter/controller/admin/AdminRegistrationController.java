package com.educenter.controller.admin;

import com.educenter.entity.Registration;
import com.educenter.security.CustomUserDetails;
import com.educenter.service.RegistrationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/registrations")
public class AdminRegistrationController {

    private final RegistrationService registrationService;

    public AdminRegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @GetMapping
    public String listRegistrations(@RequestParam(required = false) String filter, Model model) {
        List<Registration> registrations;
        if ("pending".equalsIgnoreCase(filter)) {
            registrations = registrationService.getPendingRegistrations();
        } else {
            registrations = registrationService.getAllRegistrations();
        }

        model.addAttribute("registrations", registrations);
        model.addAttribute("pendingCount", registrationService.getPendingCount());
        model.addAttribute("currentFilter", filter != null ? filter : "all");
        return "admin/registrations/list";
    }

    @PostMapping("/approve/{id}")
    public String approveRegistration(@PathVariable Long id,
                                      @RequestParam(required = false) String adminNote,
                                      @AuthenticationPrincipal CustomUserDetails userDetails,
                                      RedirectAttributes redirectAttributes) {
        try {
            Long adminId = userDetails != null ? userDetails.getId() : null;
            registrationService.approveRegistration(id, adminId, adminNote);
            redirectAttributes.addFlashAttribute("successMessage", "Đã phê duyệt đơn đăng ký! Học sinh đã được thêm vào lớp và hệ thống đã tự động tạo khoản học phí.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/registrations";
    }

    @PostMapping("/reject/{id}")
    public String rejectRegistration(@PathVariable Long id,
                                     @RequestParam(required = false) String adminNote,
                                     @AuthenticationPrincipal CustomUserDetails userDetails,
                                     RedirectAttributes redirectAttributes) {
        try {
            Long adminId = userDetails != null ? userDetails.getId() : null;
            registrationService.rejectRegistration(id, adminId, adminNote);
            redirectAttributes.addFlashAttribute("successMessage", "Đã từ chối đơn đăng ký.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/registrations";
    }
}
