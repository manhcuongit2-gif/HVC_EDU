package com.educenter.controller.student;

import com.educenter.dto.StudentHomeworkRow;
import com.educenter.security.CustomUserDetails;
import com.educenter.service.HomeworkService;
import com.educenter.service.ExamService;
import com.educenter.service.StudentPortalService;
import com.educenter.service.impl.PdfExamService;
import com.educenter.service.impl.StudentLearningProgressService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/student")
@PreAuthorize("hasRole('STUDENT')")
public class StudentPortalController {
    private final StudentPortalService portal;
    private final HomeworkService homework;
    private final ExamService exams;
    private final PdfExamService pdfExams;
    private final StudentLearningProgressService learningProgress;

    public StudentPortalController(StudentPortalService portal, HomeworkService homework, ExamService exams,
                                   PdfExamService pdfExams, StudentLearningProgressService learningProgress) {
        this.portal = portal;
        this.homework = homework;
        this.exams = exams;
        this.pdfExams = pdfExams;
        this.learningProgress = learningProgress;
    }

    @GetMapping({"", "/dashboard"})
    public String dashboard(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        model.addAttribute("currentUser", user);
        model.addAttribute("portal", portal.getPortalData(user.getId()));
        model.addAttribute("examAssignments", exams.assignmentsForStudent(user.getId()));
        model.addAttribute("pdfExamAssignments", pdfExams.forStudent(user.getId()));
        return "student/dashboard";
    }

    @GetMapping("/progress")
    public String progress(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        model.addAttribute("currentUser", user);
        var portalData = portal.getPortalData(user.getId());
        model.addAttribute("portal", portalData);
        model.addAttribute("learningProgress", learningProgress.overview(user.getId(), portalData));
        model.addAttribute("examResults", exams.publishedResultsForStudent(user.getId()));
        return "student/progress";
    }

    @GetMapping("/homework")
    public String homework(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        model.addAttribute("currentUser", user);
        model.addAttribute("rows", homework.findForStudent(user.getId()));
        return "student/homework";
    }

    @PostMapping("/homework/{id}/submit")
    public String submit(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user,
                         @RequestParam(required = false) String responseText,
                         @RequestParam(required = false) MultipartFile file,
                         RedirectAttributes redirect) {
        try {
            var submission = homework.submitForStudent(user.getId(), id, responseText, file);
            redirect.addFlashAttribute("successMessage", submission.isLate()
                    ? "Đã nộp bài. Hệ thống ghi nhận bài nộp muộn."
                    : "Đã nộp bài thành công.");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/student/homework";
    }

    @GetMapping("/homework/{id}/attachment")
    public ResponseEntity<Resource> assignmentFile(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        StudentHomeworkRow row = homework.findForStudent(user.getId()).stream()
                .filter(item -> item.assignment().getId().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài tập."));
        return response(row.assignment().getAttachmentOriginalFilename(), homework.assignmentFileForStudent(user.getId(), id),
                row.assignment().getAttachmentContentType());
    }

    @GetMapping("/homework/submission/{id}/file")
    public ResponseEntity<Resource> submissionFile(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        StudentHomeworkRow row = homework.findForStudent(user.getId()).stream()
                .filter(item -> item.submission() != null && item.submission().getId().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài nộp."));
        return response(row.submission().getAttachmentOriginalFilename(), homework.submissionFileForStudent(user.getId(), id),
                row.submission().getAttachmentContentType());
    }

    private ResponseEntity<Resource> response(String filename, java.nio.file.Path path, String contentType) {
        MediaType media = contentType == null ? MediaTypeFactory.getMediaType(filename).orElse(MediaType.APPLICATION_OCTET_STREAM)
                : MediaType.parseMediaType(contentType);
        return ResponseEntity.ok().contentType(media).header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
                .body(new FileSystemResource(path));
    }
}
