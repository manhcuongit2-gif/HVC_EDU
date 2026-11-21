package com.educenter.controller.teacher;

import com.educenter.dto.*;
import com.educenter.security.CustomUserDetails;
import com.educenter.service.AIExportService;
import com.educenter.service.ExamService;
import com.educenter.service.impl.PdfExamService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Controller
@RequestMapping("/teacher/exams")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherExamController {
    private final ExamService exams;
    private final AIExportService exports;
    private final PdfExamService pdfExams;
    public TeacherExamController(ExamService exams, AIExportService exports, PdfExamService pdfExams) {
        this.exams = exams; this.exports = exports; this.pdfExams = pdfExams;
    }

    @GetMapping("/create")
    public String createOptions() { return "teacher/exams/create"; }

    @GetMapping("/create/direct")
    public String directForm(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        DirectExamForm form = new DirectExamForm();
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).withSecond(0).withNano(0);
        form.setOpensAt(now); form.setClosesAt(now.plusDays(1)); form.getQuestions().add(new DirectExamQuestionForm());
        model.addAttribute("form", form); model.addAttribute("classes", exams.classesForTeacher(user.getId()));
        return "teacher/exams/direct-form";
    }

    @PostMapping("/create/direct")
    public String createDirect(@ModelAttribute("form") DirectExamForm form, @AuthenticationPrincipal CustomUserDetails user,
                               Model model, RedirectAttributes redirect) {
        try {
            var assignment = exams.createDirectAssignment(user.getId(), form);
            redirect.addFlashAttribute("successMessage", "Đã giao đề thi trực tuyến cho lớp.");
            return "redirect:/teacher/exams/assignments/" + assignment.getId();
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("classes", exams.classesForTeacher(user.getId()));
            if (form.getQuestions() == null || form.getQuestions().isEmpty()) {
                form.setQuestions(new ArrayList<>()); form.getQuestions().add(new DirectExamQuestionForm());
            }
            return "teacher/exams/direct-form";
        }
    }

    @GetMapping("/create/pdf")
    public String pdfForm(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        LocalDateTime due = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).withSecond(0).withNano(0).plusDays(1);
        model.addAttribute("classes", exams.classesForTeacher(user.getId()));
        model.addAttribute("dueAtText", due.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")));
        return "teacher/exams/pdf-form";
    }

    @PostMapping(value = "/create/pdf", consumes = "multipart/form-data")
    public String createPdf(@RequestParam Long classId, @RequestParam(required = false) String title,
                            @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
                            @RequestParam LocalDateTime closesAt, @RequestParam("pdf") MultipartFile pdf,
                            @AuthenticationPrincipal CustomUserDetails user, Model model, RedirectAttributes redirect) {
        try {
            var assignment = pdfExams.createForTeacher(user.getId(), classId, title, closesAt, pdf);
            redirect.addFlashAttribute("successMessage", "Đã tải và giao đề PDF cho lớp.");
            return "redirect:/teacher/exams/assignments/" + assignment.getId();
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage()); model.addAttribute("classes", exams.classesForTeacher(user.getId()));
            model.addAttribute("dueAtText", closesAt == null ? null : closesAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")));
            model.addAttribute("title", title); model.addAttribute("classId", classId);
            return "teacher/exams/pdf-form";
        }
    }

    @GetMapping({"", "/questions"})
    public String questions(@AuthenticationPrincipal CustomUserDetails user, Model model,
                            @RequestParam(required = false) Long subjectId, @RequestParam(required = false) String gradeLevel,
                            @RequestParam(required = false) String topic, @RequestParam(required = false) String questionType,
                            @RequestParam(required = false) String difficulty) {
        model.addAttribute("questions", exams.searchQuestions(user.getId(), subjectId, gradeLevel, topic, questionType, difficulty));
        model.addAttribute("filterSubjectId", subjectId); model.addAttribute("filterGradeLevel", gradeLevel);
        model.addAttribute("filterTopic", topic); model.addAttribute("filterQuestionType", questionType); model.addAttribute("filterDifficulty", difficulty);
        model.addAttribute("questionForm", new ExamQuestionForm());
        model.addAttribute("subjects", exams.subjects());
        return "teacher/exams/questions";
    }

    @GetMapping("/questions/{id}/edit")
    public String editQuestion(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user, Model model) {
        model.addAttribute("questions", exams.questionsForTeacher(user.getId()));
        model.addAttribute("questionForm", exams.questionFormForTeacher(user.getId(), id));
        model.addAttribute("subjects", exams.subjects());
        return "teacher/exams/questions";
    }

    @PostMapping("/questions")
    public String saveQuestion(@ModelAttribute ExamQuestionForm form, @AuthenticationPrincipal CustomUserDetails user,
                               RedirectAttributes redirect) {
        try { exams.saveQuestion(user.getId(), form); redirect.addFlashAttribute("successMessage", "Đã lưu câu hỏi."); }
        catch (IllegalArgumentException e) { redirect.addFlashAttribute("errorMessage", e.getMessage()); }
        return "redirect:/teacher/exams/questions";
    }

    @PostMapping("/questions/{id}/archive")
    public String archiveQuestion(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user, RedirectAttributes redirect) {
        try { exams.archiveQuestion(user.getId(), id); redirect.addFlashAttribute("successMessage", "Đã lưu trữ câu hỏi."); }
        catch (IllegalArgumentException e) { redirect.addFlashAttribute("errorMessage", e.getMessage()); }
        return "redirect:/teacher/exams/questions";
    }

    @GetMapping("/blueprints")
    public String blueprints(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        model.addAttribute("blueprints", exams.blueprintsForTeacher(user.getId()));
        return "teacher/exams/blueprints";
    }

    @GetMapping("/blueprints/new")
    public String newBlueprint() { return "redirect:/teacher/exams/create"; }

    @PostMapping("/blueprints")
    public String createBlueprint() { return "redirect:/teacher/exams/create"; }

    @GetMapping("/blueprints/{id}")
    public String previewBlueprint(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user, Model model) {
        model.addAttribute("blueprint", exams.blueprintForTeacher(user.getId(), id));
        model.addAttribute("items", exams.itemsForTeacher(user.getId(), id).stream().map(item -> {
            ExamQuestionForm form = exams.questionFormForTeacher(user.getId(), item.getQuestion().getId());
            java.util.List<StudentExamOptionView> options = new java.util.ArrayList<>();
            if (form.getOptionA() != null) options.add(new StudentExamOptionView("A", form.getOptionA()));
            if (form.getOptionB() != null) options.add(new StudentExamOptionView("B", form.getOptionB()));
            if (form.getOptionC() != null) options.add(new StudentExamOptionView("C", form.getOptionC()));
            if (form.getOptionD() != null) options.add(new StudentExamOptionView("D", form.getOptionD()));
            String answer = form.getCorrectKey() != null ? form.getCorrectKey() : form.getAcceptedAnswers();
            return new ExamBlueprintPreviewItem(item, options, answer, form.getExplanation());
        }).toList());
        model.addAttribute("pointsForm", new ExamPointsForm());
        return "teacher/exams/blueprint-preview";
    }

    @PostMapping("/blueprints/{id}/points")
    public String updatePoints(@PathVariable Long id, @ModelAttribute ExamPointsForm form,
                               @AuthenticationPrincipal CustomUserDetails user, RedirectAttributes redirect) {
        try { exams.updateItemPoints(user.getId(), id, form.getPoints()); redirect.addFlashAttribute("successMessage", "Đã cập nhật điểm từng câu."); }
        catch (IllegalArgumentException e) { redirect.addFlashAttribute("errorMessage", e.getMessage()); }
        return "redirect:/teacher/exams/blueprints/" + id;
    }

    @PostMapping("/blueprints/{id}/items/{itemId}/replace")
    public String replaceItem(@PathVariable Long id, @PathVariable Long itemId,
                              @AuthenticationPrincipal CustomUserDetails user, RedirectAttributes redirect) {
        try { exams.replaceItem(user.getId(), id, itemId); redirect.addFlashAttribute("successMessage", "Đã thay câu hỏi."); }
        catch (IllegalArgumentException e) { redirect.addFlashAttribute("errorMessage", e.getMessage()); }
        return "redirect:/teacher/exams/blueprints/" + id;
    }

    @GetMapping("/blueprints/{id}/assign")
    public String assignForm(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user, Model model) {
        var blueprint = exams.blueprintForTeacher(user.getId(), id); ExamAssignmentForm form = new ExamAssignmentForm();
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).withSecond(0).withNano(0);
        form.setOpensAt(now); form.setClosesAt(now.plusDays(1)); form.setDurationMinutes(blueprint.getDurationMinutes());
        if (blueprint.isPreserveQuestionOrder()) form.setShuffleQuestions(false);
        model.addAttribute("blueprint", blueprint); model.addAttribute("form", form); model.addAttribute("classes", exams.classesForTeacher(user.getId()));
        return "teacher/exams/assign-form";
    }

    @PostMapping("/blueprints/{id}/assign")
    public String assign(@PathVariable Long id, @ModelAttribute("form") ExamAssignmentForm form,
                         @AuthenticationPrincipal CustomUserDetails user, Model model, RedirectAttributes redirect) {
        try {
            var assignment = exams.assign(user.getId(), id, form); redirect.addFlashAttribute("successMessage", "Đã giao đề cho lớp.");
            return "redirect:/teacher/exams/assignments/" + assignment.getId();
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage()); model.addAttribute("blueprint", exams.blueprintForTeacher(user.getId(), id));
            model.addAttribute("form", form); model.addAttribute("classes", exams.classesForTeacher(user.getId()));
            return "teacher/exams/assign-form";
        }
    }

    @GetMapping("/assignments")
    public String assignments(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        model.addAttribute("assignments", exams.assignmentsForTeacher(user.getId())); return "teacher/exams/assignments";
    }

    @GetMapping("/assignments/{id}")
    public String assignment(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user, Model model) {
        var assignment = exams.assignmentForTeacher(user.getId(), id);
        if (assignment.isPdfExam()) {
            model.addAttribute("assignment", assignment);
            model.addAttribute("submissions", pdfExams.submissionsForTeacher(user.getId(), id));
            model.addAttribute("gradeForm", new ExamGradeForm());
            return "teacher/exams/pdf-assignment";
        }
        model.addAttribute("report", exams.reportForTeacher(user.getId(), id));
        model.addAttribute("assignment", assignment);
        return "teacher/exams/assignment";
    }

    @GetMapping("/assignments/{id}/pdf")
    public ResponseEntity<Resource> teacherPdf(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        return inlineFile(pdfExams.pdfForTeacher(user.getId(), id));
    }

    @GetMapping("/assignments/{id}/pdf-submission-files/{fileId}")
    public ResponseEntity<Resource> teacherSubmissionFile(@PathVariable Long id, @PathVariable Long fileId,
                                                          @AuthenticationPrincipal CustomUserDetails user) {
        return inlineFile(pdfExams.submissionFileForTeacher(user.getId(), id, fileId));
    }

    @PostMapping("/assignments/{id}/pdf-submissions/{submissionId}/grade")
    public String gradePdf(@PathVariable Long id, @PathVariable Long submissionId, @ModelAttribute ExamGradeForm form,
                           @AuthenticationPrincipal CustomUserDetails user, RedirectAttributes redirect) {
        try {
            pdfExams.grade(user.getId(), id, submissionId, form.getScore(), form.getFeedback());
            redirect.addFlashAttribute("successMessage", "Đã lưu điểm và nhận xét cho bài PDF.");
        } catch (IllegalArgumentException e) { redirect.addFlashAttribute("errorMessage", e.getMessage()); }
        return "redirect:/teacher/exams/assignments/" + id;
    }

    @GetMapping("/assignments/{id}/attempts/{attemptId}")
    public String attempt(@PathVariable Long id, @PathVariable Long attemptId,
                          @AuthenticationPrincipal CustomUserDetails user, Model model) {
        model.addAttribute("assignment", exams.assignmentForTeacher(user.getId(), id));
        model.addAttribute("attempt", exams.attemptForTeacher(user.getId(), id, attemptId));
        return "teacher/exams/attempt-review";
    }

    @PostMapping("/assignments/{id}/attempts/{attemptId}/questions/{number}/grade")
    public String gradeEssay(@PathVariable Long id, @PathVariable Long attemptId, @PathVariable int number,
                             @ModelAttribute ExamGradeForm form, @AuthenticationPrincipal CustomUserDetails user,
                             RedirectAttributes redirect) {
        try { exams.gradeEssay(user.getId(), id, attemptId, number, form.getScore(), form.getFeedback()); redirect.addFlashAttribute("successMessage", "Đã lưu điểm và nhận xét."); }
        catch (IllegalArgumentException e) { redirect.addFlashAttribute("errorMessage", e.getMessage()); }
        return "redirect:/teacher/exams/assignments/" + id + "/attempts/" + attemptId;
    }

    @PostMapping("/assignments/{id}/publish")
    public String publish(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user, RedirectAttributes redirect) {
        try { exams.publishResults(user.getId(), id); redirect.addFlashAttribute("successMessage", "Đã công bố kết quả."); }
        catch (IllegalArgumentException e) { redirect.addFlashAttribute("errorMessage", e.getMessage()); }
        return "redirect:/teacher/exams/assignments/" + id;
    }

    @GetMapping("/assignments/{id}/export")
    public ResponseEntity<Resource> export(@PathVariable Long id, @RequestParam(defaultValue = "word") String format,
                                           @RequestParam(defaultValue = "false") boolean answers,
                                           @AuthenticationPrincipal CustomUserDetails user) {
        ExamExportDocument doc = exams.exportForTeacher(user.getId(), id, answers);
        boolean pdf = "pdf".equalsIgnoreCase(format);
        byte[] bytes = pdf ? exports.exportExamPdf(doc) : exports.exportExamWord(doc);
        String filename = safeFilename(doc.title()) + (pdf ? ".pdf" : ".docx");
        MediaType media = pdf ? MediaType.APPLICATION_PDF : MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        return ResponseEntity.ok().contentType(media).header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString()).body(new ByteArrayResource(bytes));
    }

    private String safeFilename(String value) { return value.replaceAll("[^\\p{L}\\p{N}._ -]", "").trim().replace(' ', '-'); }

    private ResponseEntity<Resource> inlineFile(SecureExamFile file) {
        return ResponseEntity.ok().contentType(org.springframework.http.MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(file.originalFilename(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff").body(new FileSystemResource(file.path()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Void> invalidExamReference() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String uploadTooLarge(RedirectAttributes redirect) {
        redirect.addFlashAttribute("errorMessage", "Tệp vượt giới hạn tải lên hiện tại (20 MB mỗi tệp, 22 MB mỗi yêu cầu).");
        return "redirect:/teacher/exams/create/pdf";
    }
}
