package com.educenter.controller.student;

import com.educenter.config.AIProperties;
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
    private final AIProperties properties;

    public StudentAIPageController(AIProperties properties) { this.properties = properties; }

    @GetMapping
    public String index(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        model.addAttribute("currentUser", user);
        model.addAttribute("maxUploadBytes", properties.getGroq().getMaxUploadBytes());
        return "student/ai";
    }
}
