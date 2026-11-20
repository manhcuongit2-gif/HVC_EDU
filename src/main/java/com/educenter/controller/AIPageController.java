package com.educenter.controller;

import com.educenter.entity.ClassEntity;
import com.educenter.entity.Parent;
import com.educenter.repository.ClassRepository;
import com.educenter.repository.ParentRepository;
import com.educenter.repository.StudentRepository;
import com.educenter.security.CustomUserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;

@Controller
@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'PARENT')")
public class AIPageController {
    private final ParentRepository parentRepository;
    private final StudentRepository studentRepository;
    private final ClassRepository classRepository;

    public AIPageController(ParentRepository parentRepository, StudentRepository studentRepository,
                            ClassRepository classRepository) {
        this.parentRepository = parentRepository;
        this.studentRepository = studentRepository;
        this.classRepository = classRepository;
    }

    @GetMapping("/ai")
    public String index(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        model.addAttribute("currentUser", user);
        model.addAttribute("roleName", user.getRoleName());
        if ("ROLE_PARENT".equals(user.getRoleName())) {
            Parent parent = parentRepository.findByUserId(user.getId()).orElse(null);
            model.addAttribute("aiChildren", parent == null ? List.of() : studentRepository.findByParentId(parent.getId()));
        }
        if ("ROLE_TEACHER".equals(user.getRoleName())) {
            List<ClassEntity> classes = classRepository.findAll().stream().filter(c ->
                    c.getTeacher() != null && c.getTeacher().getUser() != null && user.getId().equals(c.getTeacher().getUser().getId())
                            || c.getAssistantTeacher() != null && c.getAssistantTeacher().getUser() != null
                            && user.getId().equals(c.getAssistantTeacher().getUser().getId())).toList();
            model.addAttribute("aiClasses", classes);
        }
        return "ai/index";
    }
}
