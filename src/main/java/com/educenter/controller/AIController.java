package com.educenter.controller;

import com.educenter.config.AIProperties;
import com.educenter.dto.*;
import com.educenter.entity.AISettings;
import com.educenter.security.CustomUserDetails;
import com.educenter.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/api/ai")
@Validated
@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'PARENT')")
public class AIController {
    private final AIConversationService conversations;
    private final AIService aiService;
    private final AIUsageService usageService;
    private final AIEducationService educationService;
    private final AIDocumentService documentService;
    private final AIExportService exportService;
    private final AIChatAssistService chatAssistService;
    private final AIProperties properties;
    private final ObjectMapper objectMapper;

    public AIController(AIConversationService conversations, AIService aiService, AIUsageService usageService,
                        AIEducationService educationService, AIDocumentService documentService,
                        AIExportService exportService, AIChatAssistService chatAssistService,
                        AIProperties properties, ObjectMapper objectMapper) {
        this.conversations = conversations;
        this.aiService = aiService;
        this.usageService = usageService;
        this.educationService = educationService;
        this.documentService = documentService;
        this.exportService = exportService;
        this.chatAssistService = chatAssistService;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/availability")
    @ResponseBody
    public Map<String, Object> availability(@AuthenticationPrincipal CustomUserDetails user) {
        AISettings settings = usageService.settings();
        boolean roleEnabled = switch (user.getRoleName()) {
            case "ROLE_ADMIN" -> settings.isAdminEnabled();
            case "ROLE_TEACHER" -> settings.isTeacherEnabled();
            case "ROLE_PARENT" -> settings.isParentEnabled();
            default -> false;
        };
        return Map.of("configured", properties.isEnabled() && properties.getGroq().hasApiKey(),
                "enabled", settings.isEnabled() && roleEnabled,
                "ready", properties.isEnabled() && properties.getGroq().hasApiKey() && settings.isEnabled() && roleEnabled,
                "usage", usageService.summary(user.getId()));
    }

    @GetMapping("/conversations")
    @ResponseBody
    public List<AIConversationSummary> list(@AuthenticationPrincipal CustomUserDetails user,
                                             @RequestParam(required = false) String search) {
        return conversations.list(user.getId(), search);
    }

    @PostMapping("/conversations")
    @ResponseBody
    public AIConversationSummary create(@AuthenticationPrincipal CustomUserDetails user) {
        var conversation = conversations.create(user.getId());
        return new AIConversationSummary(conversation.getId(), conversation.getTitle(), conversation.isPinned(),
                conversation.getUpdatedAt(), "", conversation.getContextRole(), conversation.getContextId());
    }

    @GetMapping("/conversations/{id}/messages")
    @ResponseBody
    public List<AIMessageView> messages(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        return conversations.messages(id, user.getId());
    }

    @PatchMapping("/conversations/{id}")
    @ResponseBody
    public AIConversationSummary update(@PathVariable Long id, @RequestBody AIConversationUpdate update,
                                        @AuthenticationPrincipal CustomUserDetails user) {
        return conversations.update(id, user.getId(), update.title(), update.pinned());
    }

    @DeleteMapping("/conversations/{id}")
    @ResponseBody
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        conversations.delete(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> chat(@AuthenticationPrincipal CustomUserDetails user,
                                                       @RequestBody @Valid AIChatRequest request) {
        StreamingResponseBody body = output -> {
            try {
                aiService.chat(user.getId(), user.getRoleName(), request, event -> writeEvent(output, event));
            } catch (UncheckedIOException disconnected) {
                // A client that stopped generation has closed the stream.
            } catch (AIException error) {
                writeEvent(output, new AIService.AIStreamEvent("error", Map.of("code", error.getCode(), "message", error.getUserMessage())));
            } catch (IllegalArgumentException error) {
                writeEvent(output, new AIService.AIStreamEvent("error", Map.of("code", "INVALID_REQUEST", "message", error.getMessage())));
            } catch (Exception error) {
                writeEvent(output, new AIService.AIStreamEvent("error", Map.of("code", "PROCESSING_ERROR", "message", "Không thể xử lý yêu cầu AI lúc này.")));
            }
        };
        return ResponseEntity.ok().contentType(MediaType.TEXT_EVENT_STREAM)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .header("X-Accel-Buffering", "no")
                .body(body);
    }

    @GetMapping("/usage")
    @ResponseBody
    public AIUsageSummary usage(@AuthenticationPrincipal CustomUserDetails user) {
        return usageService.summary(user.getId());
    }

    @PostMapping("/chat-assist")
    @ResponseBody
    public Map<String, String> chatAssist(@AuthenticationPrincipal CustomUserDetails user,
                                          @RequestBody @Valid AIChatAssistRequest request) {
        return Map.of("draft", chatAssistService.createDraft(user.getId(), user.getRoleName(),
                request.conversationId(), request.action(), request.draft()));
    }

    @PostMapping("/generate-report")
    @ResponseBody
    @PreAuthorize("hasRole('TEACHER')")
    public Map<Long, String> generateReport(@AuthenticationPrincipal CustomUserDetails user,
                                            @RequestBody @Valid AIReportRequest request) {
        return educationService.generateReports(user.getId(), request);
    }

    @PostMapping("/generate-exercises")
    @ResponseBody
    @PreAuthorize("hasRole('TEACHER')")
    public Map<String, String> generateExercises(@AuthenticationPrincipal CustomUserDetails user,
                                                  @RequestBody @Valid AIExerciseRequest request) {
        return Map.of("content", educationService.generateExercises(user.getId(), request));
    }

    @PostMapping("/generate-exam")
    @ResponseBody
    @PreAuthorize("hasRole('TEACHER')")
    public Map<String, String> generateExam(@AuthenticationPrincipal CustomUserDetails user,
                                             @RequestBody @Valid AIExamRequest request) {
        return Map.of("content", educationService.generateExam(user.getId(), request));
    }

    @PostMapping(value = "/documents/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    @PreAuthorize("hasRole('TEACHER')")
    public Map<String, String> extractDocument(@RequestPart("file") MultipartFile file) {
        return Map.of("text", documentService.extractText(file));
    }

    @PostMapping("/export")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ByteArrayResource> export(@RequestBody @Valid AIExportRequest request) {
        String format = request.format().toLowerCase(java.util.Locale.ROOT);
        byte[] bytes;
        String filename;
        MediaType type;
        if ("docx".equals(format) || "word".equals(format)) {
            bytes = exportService.exportWord(request.title(), request.content());
            filename = "hung-cuong-ai.docx";
            type = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        } else if ("pdf".equals(format)) {
            bytes = exportService.exportPdf(request.title(), request.content());
            filename = "hung-cuong-ai.pdf";
            type = MediaType.APPLICATION_PDF;
        } else throw new IllegalArgumentException("Chỉ hỗ trợ xuất Word hoặc PDF.");
        return ResponseEntity.ok().contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
                .body(new ByteArrayResource(bytes));
    }

    @ExceptionHandler(AIException.class)
    @ResponseBody
    public ResponseEntity<Map<String, String>> aiException(AIException error) {
        int status = error.getProviderStatus() != null && error.getProviderStatus() == 429 ? 429
                : SetOfClientErrors.contains(error.getCode()) ? 400 : 503;
        return ResponseEntity.status(status).body(Map.of("code", error.getCode(), "message", error.getUserMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException error) {
        return ResponseEntity.badRequest().body(Map.of("code", "INVALID_REQUEST", "message", error.getMessage()));
    }

    private static final java.util.Set<String> SetOfClientErrors = java.util.Set.of("PROMPT_LIMIT", "USER_DAILY_LIMIT",
            "GLOBAL_DAILY_LIMIT", "TOKEN_DAILY_LIMIT", "CONCURRENT_LIMIT", "ROLE_DISABLED", "AI_DISABLED", "AI_UNAVAILABLE");

    private void writeEvent(java.io.OutputStream output, AIService.AIStreamEvent event) {
        try {
            output.write(("event: " + event.type() + "\ndata: " + objectMapper.writeValueAsString(event.data()) + "\n\n")
                    .getBytes(StandardCharsets.UTF_8));
            output.flush();
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }
}
