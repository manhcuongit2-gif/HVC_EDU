package com.educenter.controller.teacher;

import com.educenter.entity.HomeworkAssignment;
import com.educenter.entity.HomeworkSubmission;
import com.educenter.repository.ClassRepository;
import com.educenter.security.CustomUserDetails;
import com.educenter.service.HomeworkService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/teacher/homework")
public class TeacherHomeworkController {
    private final HomeworkService homework;
    private final ClassRepository classes;

    public TeacherHomeworkController(HomeworkService homework, ClassRepository classes) { this.homework = homework; this.classes = classes; }

    @GetMapping
    public String index(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        model.addAttribute("currentUser", user); model.addAttribute("classes", classesFor(user.getId()));
        model.addAttribute("assignments", homework.findForTeacher(user.getId())); return "teacher/homework/index";
    }

    @PostMapping
    public String create(@AuthenticationPrincipal CustomUserDetails user, @RequestParam Long classId,
                         @RequestParam String title, @RequestParam String instructions, @RequestParam LocalDateTime dueAt,
                         @RequestParam(required = false) MultipartFile attachment, RedirectAttributes redirect) {
        try { homework.createForTeacher(user.getId(), classId, title, instructions, dueAt, attachment); redirect.addFlashAttribute("successMessage", "Đã giao bài tập cho lớp."); }
        catch (IllegalArgumentException e) { redirect.addFlashAttribute("errorMessage", e.getMessage()); }
        return "redirect:/teacher/homework";
    }

    @GetMapping("/{id}/submissions")
    public String submissions(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user, Model model) {
        HomeworkAssignment assignment = homework.findForTeacher(user.getId()).stream().filter(a -> a.getId().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài tập."));
        model.addAttribute("currentUser", user); model.addAttribute("assignment", assignment);
        model.addAttribute("submissions", homework.submissionsForTeacher(user.getId(), id)); return "teacher/homework/submissions";
    }

    @PostMapping("/{id}/submissions/{submissionId}/review")
    public String review(@PathVariable Long id, @PathVariable Long submissionId, @AuthenticationPrincipal CustomUserDetails user,
                         @RequestParam BigDecimal score, @RequestParam(required = false) String feedback, RedirectAttributes redirect) {
        try { homework.reviewForTeacher(user.getId(), id, submissionId, score, feedback); redirect.addFlashAttribute("successMessage", "Đã lưu điểm và nhận xét."); }
        catch (IllegalArgumentException e) { redirect.addFlashAttribute("errorMessage", e.getMessage()); }
        return "redirect:/teacher/homework/" + id + "/submissions";
    }

    @GetMapping("/{id}/attachment")
    public ResponseEntity<Resource> assignmentFile(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        HomeworkAssignment assignment = homework.findForTeacher(user.getId()).stream().filter(a -> a.getId().equals(id)).findFirst().orElseThrow();
        return response(assignment.getAttachmentOriginalFilename(), homework.assignmentFileForTeacher(user.getId(), id), assignment.getAttachmentContentType());
    }

    @GetMapping("/{id}/submissions/{submissionId}/file")
    public ResponseEntity<Resource> submissionFile(@PathVariable Long id, @PathVariable Long submissionId, @AuthenticationPrincipal CustomUserDetails user) {
        HomeworkSubmission submission = homework.submissionsForTeacher(user.getId(), id).stream().filter(s -> s.getId().equals(submissionId)).findFirst().orElseThrow();
        return response(submission.getAttachmentOriginalFilename(), homework.submissionFileForTeacher(user.getId(), id, submissionId), submission.getAttachmentContentType());
    }

    private java.util.List<com.educenter.entity.ClassEntity> classesFor(Long teacherId) {
        return classes.findAll().stream().filter(c -> (c.getTeacher() != null && c.getTeacher().getUser().getId().equals(teacherId))
                || (c.getAssistantTeacher() != null && c.getAssistantTeacher().getUser().getId().equals(teacherId))).toList();
    }
    private ResponseEntity<Resource> response(String filename, java.nio.file.Path path, String contentType) {
        MediaType media = contentType == null ? MediaTypeFactory.getMediaType(filename).orElse(MediaType.APPLICATION_OCTET_STREAM) : MediaType.parseMediaType(contentType);
        return ResponseEntity.ok().contentType(media).header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString()).body(new FileSystemResource(path));
    }
}
