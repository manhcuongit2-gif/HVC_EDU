package com.educenter.controller.parent;

import com.educenter.security.CustomUserDetails;
import com.educenter.service.ExamService;
import com.educenter.service.impl.PdfExamService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/parent/exams")
@PreAuthorize("hasRole('PARENT')")
public class ParentExamController {
    private final ExamService exams;
    private final PdfExamService pdfExams;
    public ParentExamController(ExamService exams, PdfExamService pdfExams) { this.exams = exams; this.pdfExams = pdfExams; }
    @GetMapping
    public String results(@AuthenticationPrincipal CustomUserDetails user, @RequestParam(required = false) Long childId, Model model) {
        try {
            model.addAttribute("assignments", exams.publishedResultsForParent(user.getId(), childId));
            model.addAttribute("pdfAssignments", pdfExams.gradedForParent(user.getId(), childId));
            return "parent/exams";
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
