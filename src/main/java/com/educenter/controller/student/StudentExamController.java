package com.educenter.controller.student;

import com.educenter.security.CustomUserDetails;
import com.educenter.service.ExamService;
import com.educenter.service.impl.PdfExamService;
import com.educenter.dto.SecureExamFile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/student/exams")
@PreAuthorize("hasRole('STUDENT')")
public class StudentExamController {
    private final ExamService exams;
    private final PdfExamService pdfExams;
    public StudentExamController(ExamService exams, PdfExamService pdfExams) { this.exams = exams; this.pdfExams = pdfExams; }

    @GetMapping
    public String index(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        model.addAttribute("currentUser", user);
        model.addAttribute("assignments", exams.assignmentsForStudent(user.getId()));
        model.addAttribute("pdfAssignments", pdfExams.forStudent(user.getId()));
        return "student/exams";
    }

    @GetMapping("/{assignmentId}/pdf")
    public ResponseEntity<Resource> pdf(@PathVariable Long assignmentId, @AuthenticationPrincipal CustomUserDetails user) {
        return inlineFile(pdfExams.pdfForStudent(user.getId(), assignmentId));
    }

    @GetMapping("/{assignmentId}/pdf-submission-files/{fileId}")
    public ResponseEntity<Resource> pdfSubmissionFile(@PathVariable Long assignmentId, @PathVariable Long fileId,
                                                       @AuthenticationPrincipal CustomUserDetails user) {
        return inlineFile(pdfExams.submissionFileForStudent(user.getId(), assignmentId, fileId));
    }

    @PostMapping(value = "/{assignmentId}/pdf-submissions", consumes = "multipart/form-data")
    public String submitPdf(@PathVariable Long assignmentId, @RequestParam("files") List<MultipartFile> files,
                            @AuthenticationPrincipal CustomUserDetails user, RedirectAttributes redirect) {
        try {
            pdfExams.submitForStudent(user.getId(), assignmentId, files);
            redirect.addFlashAttribute("successMessage", "Đã nộp bài làm. Trạng thái hiện tại: Chờ chấm.");
        } catch (IllegalArgumentException e) { redirect.addFlashAttribute("errorMessage", e.getMessage()); }
        return "redirect:/student/exams";
    }

    @PostMapping("/{assignmentId}/start")
    public String start(@PathVariable Long assignmentId, @AuthenticationPrincipal CustomUserDetails user, RedirectAttributes redirect) {
        try { var attempt = exams.startOrResume(user.getId(), assignmentId); return "redirect:/student/exams/attempt/" + attempt.attemptId(); }
        catch (IllegalArgumentException e) { redirect.addFlashAttribute("errorMessage", e.getMessage()); return "redirect:/student/exams"; }
    }

    @GetMapping("/attempt/{attemptId}")
    public String attempt(@PathVariable Long attemptId, @AuthenticationPrincipal CustomUserDetails user, Model model,
                          RedirectAttributes redirect) {
        try {
            model.addAttribute("attempt", exams.attemptForStudent(user.getId(), attemptId));
            return "student/exam-attempt";
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("errorMessage", "Không tìm thấy lượt thi hoặc bạn không có quyền xem.");
            return "redirect:/student/exams";
        }
    }

    @PostMapping("/attempt/{attemptId}/answer")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveAnswer(@PathVariable Long attemptId,
            @AuthenticationPrincipal CustomUserDetails user, @RequestParam int questionNumber,
            @RequestParam(required = false) String answerText, @RequestParam(required = false) String optionKey) {
        try {
            boolean saved = exams.saveAnswer(user.getId(), attemptId, questionNumber, answerText, optionKey);
            return saved ? ResponseEntity.ok(Map.of("saved", true, "submitted", false))
                    : ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("saved", false, "submitted", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("saved", false, "submitted", false, "message", "Không thể lưu câu trả lời này."));
        }
    }

    @PostMapping("/attempt/{attemptId}/submit")
    public String submit(@PathVariable Long attemptId, @AuthenticationPrincipal CustomUserDetails user, RedirectAttributes redirect) {
        try { exams.submitAttempt(user.getId(), attemptId); redirect.addFlashAttribute("successMessage", "Đã nộp bài thi."); }
        catch (IllegalArgumentException e) { redirect.addFlashAttribute("errorMessage", e.getMessage()); }
        return "redirect:/student/exams";
    }

    @GetMapping("/results")
    public String results(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        model.addAttribute("currentUser", user);
        model.addAttribute("assignments", exams.publishedResultsForStudent(user.getId())); return "student/exam-results";
    }

    private ResponseEntity<Resource> inlineFile(SecureExamFile file) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(file.originalFilename(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff").body(new FileSystemResource(file.path()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String uploadTooLarge(RedirectAttributes redirect) {
        redirect.addFlashAttribute("errorMessage", "Ảnh vượt giới hạn tải lên hiện tại (20 MB mỗi tệp, 22 MB mỗi yêu cầu; tổng ảnh tối đa 20 MB).");
        return "redirect:/student/exams";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Void> invalidExamReference() { return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); }
}
