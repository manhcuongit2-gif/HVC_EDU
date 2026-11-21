package com.educenter.controller.teacher;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** Keeps old bookmarks working while retiring the document-to-matrix import flow. */
@Controller
@RequestMapping("/teacher/exams/import")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherExamImportController {
    @PostMapping({"/preview", "/save"})
    public String retiredImport() { return "redirect:/teacher/exams/create"; }
}
