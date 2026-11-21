package com.educenter.controller.student;

import com.educenter.security.CustomUserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/student/ai")
@PreAuthorize("hasRole('STUDENT')")
public class StudentAIPageController {
    @GetMapping
    public String index(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        model.addAttribute("currentUser", user);
        return "student/ai";
    }
}
