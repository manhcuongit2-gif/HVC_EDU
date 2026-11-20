package com.educenter.controller.parent;

import com.educenter.dto.ParentHomeworkRow;
import com.educenter.entity.HomeworkSubmission;
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

import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/parent/homework")
public class ParentHomeworkController {
    private final HomeworkService homework;
    public ParentHomeworkController(HomeworkService homework) { this.homework = homework; }

    @GetMapping
    public String index(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        model.addAttribute("currentUser", user); model.addAttribute("rows", homework.findForParent(user.getId())); return "parent/homework/index";
    }

    @PostMapping("/{id}/submit")
    public String submit(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user,
                         @RequestParam Long studentId, @RequestParam MultipartFile file, RedirectAttributes redirect) {
        try {
            HomeworkSubmission result = homework.submitForParent(user.getId(), id, studentId, file);
            redirect.addFlashAttribute("successMessage", result.isLate() ? "Đã nộp bài (hệ thống ghi nhận nộp muộn)." : "Đã nộp bài thành công.");
        } catch (IllegalArgumentException e) { redirect.addFlashAttribute("errorMessage", e.getMessage()); }
        return "redirect:/parent/homework";
    }

    @GetMapping("/{id}/attachment")
    public ResponseEntity<Resource> assignmentFile(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        ParentHomeworkRow row = homework.findForParent(user.getId()).stream().filter(r -> r.assignment().getId().equals(id)).findFirst().orElseThrow();
        return response(row.assignment().getAttachmentOriginalFilename(), homework.assignmentFileForParent(user.getId(), id), row.assignment().getAttachmentContentType());
    }

    @GetMapping("/submission/{id}/file")
    public ResponseEntity<Resource> submissionFile(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        HomeworkSubmission submission = homework.findForParent(user.getId()).stream().map(ParentHomeworkRow::submission)
                .filter(s -> s != null && s.getId().equals(id)).findFirst().orElseThrow();
        return response(submission.getAttachmentOriginalFilename(), homework.submissionFileForParent(user.getId(), id), submission.getAttachmentContentType());
    }

    private ResponseEntity<Resource> response(String filename, java.nio.file.Path path, String contentType) {
        MediaType media = contentType == null ? MediaTypeFactory.getMediaType(filename).orElse(MediaType.APPLICATION_OCTET_STREAM) : MediaType.parseMediaType(contentType);
        return ResponseEntity.ok().contentType(media).header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString()).body(new FileSystemResource(path));
    }
}
