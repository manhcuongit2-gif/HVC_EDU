package com.educenter.controller.student;

import com.educenter.config.AIProperties;
import com.educenter.dto.*;
import com.educenter.entity.AISettings;
import com.educenter.security.CustomUserDetails;
import com.educenter.service.*;
import com.educenter.service.impl.StudentAIService;
import com.educenter.service.impl.StudentLearningProgressService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/api/ai/student")
@PreAuthorize("hasRole('STUDENT')")
public class StudentAIController {
    private final AIConversationService conversations;
    private final AIUsageService usage;
    private final AIProperties properties;
    private final StudentAIService studentAI;
    private final StudentLearningProgressService learningProgress;
    private final ObjectMapper objectMapper;

    public StudentAIController(AIConversationService conversations, AIUsageService usage, AIProperties properties,
                               StudentAIService studentAI, StudentLearningProgressService learningProgress,
                               ObjectMapper objectMapper) {
        this.conversations = conversations;
        this.usage = usage;
        this.properties = properties;
        this.studentAI = studentAI;
        this.learningProgress = learningProgress;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/availability")
    @ResponseBody
    public Map<String, Object> availability(@AuthenticationPrincipal CustomUserDetails user) {
        AISettings settings = usage.settings();
        boolean configured = properties.isEnabled() && properties.getGroq().hasApiKey();
        boolean limitsConfigured = settings.getPerUserDailyRequests() != null && settings.getPerUserDailyRequests() > 0
                && settings.getGlobalDailyRequests() != null && settings.getGlobalDailyRequests() > 0
                && settings.getDailyTokenLimit() != null && settings.getDailyTokenLimit() > 0
                && settings.getMaxPromptCharacters() != null && settings.getMaxPromptCharacters() > 0
                && settings.getMaxResponseTokens() != null && settings.getMaxResponseTokens() > 0
                && settings.getMaxConcurrentRequests() != null && settings.getMaxConcurrentRequests() > 0;
        boolean selectedModelAllowed = settings.getSelectedModel() != null
                && java.util.Arrays.stream(properties.getGroq().getAllowedModels().split(","))
                .map(String::trim).anyMatch(settings.getSelectedModel()::equals);
        boolean ready = configured && settings.isEnabled() && settings.isStudentEnabled() && limitsConfigured && selectedModelAllowed;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("configured", configured);
        result.put("enabled", settings.isEnabled() && settings.isStudentEnabled());
        result.put("limitsConfigured", limitsConfigured);
        result.put("selectedModelAllowed", selectedModelAllowed);
        result.put("ready", ready);
        result.put("selectedModel", settings.getSelectedModel());
        result.put("visionReady", ready && properties.getGroq().isVisionModel(settings.getSelectedModel()));
        result.put("usage", usage.summary(user.getId()));
        result.put("message", !configured ? "Hùng Cường AI chưa được cấu hình khóa API trên máy chủ."
                : !settings.isEnabled() ? "AI đang tắt hoặc chưa được Admin cấu hình hạn mức."
                : !settings.isStudentEnabled() ? "Admin chưa bật quyền AI cho học sinh."
                : !limitsConfigured ? "Admin cần lưu hạn mức sử dụng AI trước khi học sinh bắt đầu."
                : !selectedModelAllowed ? "Model hiện tại không còn nằm trong danh sách model được phép. Admin cần chọn lại model."
                : "Hùng Cường AI sẵn sàng.");
        return result;
    }

    @GetMapping("/conversations")
    @ResponseBody
    public List<AIConversationSummary> list(@AuthenticationPrincipal CustomUserDetails user,
                                            @RequestParam(required = false) String search) {
        return conversations.list(user.getId(), search).stream()
                .filter(item -> "ROLE_STUDENT".equals(item.contextRole()))
                .toList();
    }

    @PostMapping("/conversations")
    @ResponseBody
    public AIConversationSummary create(@AuthenticationPrincipal CustomUserDetails user) {
        var conversation = conversations.create(user.getId(), "ROLE_STUDENT", null);
        return new AIConversationSummary(conversation.getId(), conversation.getTitle(), conversation.isPinned(),
                conversation.getUpdatedAt(), "", conversation.getContextRole(), conversation.getContextId());
    }

    @GetMapping("/conversations/{id}/messages")
    @ResponseBody
    public List<AIMessageView> messages(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        requireStudentConversation(id, user.getId());
        return conversations.messages(id, user.getId());
    }

    @PatchMapping("/conversations/{id}")
    @ResponseBody
    public AIConversationSummary update(@PathVariable Long id, @RequestBody AIConversationUpdate update,
                                        @AuthenticationPrincipal CustomUserDetails user) {
        requireStudentConversation(id, user.getId());
        return conversations.update(id, user.getId(), update.title(), update.pinned());
    }

    @DeleteMapping("/conversations/{id}")
    @ResponseBody
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        requireStudentConversation(id, user.getId());
        conversations.delete(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/usage")
    @ResponseBody
    public AIUsageSummary usage(@AuthenticationPrincipal CustomUserDetails user) {
        return usage.summary(user.getId());
    }

    @PostMapping("/study-plan/refresh")
    @ResponseBody
    public StudentStudyPlanRefreshResult refreshStudyPlan(@AuthenticationPrincipal CustomUserDetails user) {
        return learningProgress.refreshPlan(user.getId());
    }

    @PatchMapping("/study-plan/items/{id}")
    @ResponseBody
    public Map<String, Boolean> updateStudyPlanItem(@PathVariable Long id, @RequestBody Map<String, String> request,
                                                     @AuthenticationPrincipal CustomUserDetails user) {
        learningProgress.updateItemStatus(user.getId(), id, request.get("status"));
        return Map.of("saved", true);
    }

    @PostMapping(value = "/chat", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> chat(@AuthenticationPrincipal CustomUserDetails user,
                                                       @RequestParam(required = false) Long conversationId,
                                                       @RequestParam(required = false) String message,
                                                       @RequestParam(defaultValue = "ASK") String action,
                                                       @RequestPart(value = "image", required = false) MultipartFile image) {
        StudentAIChatRequest request = new StudentAIChatRequest(conversationId, message, action);
        StreamingResponseBody body = output -> {
            try {
                studentAI.chat(user.getId(), request, image, event -> writeEvent(output, event));
            } catch (UncheckedIOException disconnected) {
                // The browser closed its stream.
            } catch (AIException error) {
                writeEvent(output, new AIService.AIStreamEvent("error", Map.of("code", error.getCode(), "message", error.getUserMessage())));
            } catch (IllegalArgumentException error) {
                writeEvent(output, new AIService.AIStreamEvent("error", Map.of("code", "INVALID_REQUEST", "message", error.getMessage())));
            } catch (Exception error) {
                writeEvent(output, new AIService.AIStreamEvent("error", Map.of("code", "PROCESSING_ERROR", "message", "Không thể xử lý yêu cầu lúc này.")));
            }
        };
        return ResponseEntity.ok().contentType(MediaType.TEXT_EVENT_STREAM)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .header("X-Accel-Buffering", "no").body(body);
    }

    private void requireStudentConversation(Long id, Long userId) {
        var conversation = conversations.requireOwned(id, userId);
        if (!"ROLE_STUDENT".equals(conversation.getContextRole()))
            throw new IllegalArgumentException("Không tìm thấy cuộc trò chuyện của học sinh.");
    }

    private void writeEvent(java.io.OutputStream output, AIService.AIStreamEvent event) {
        try {
            output.write(("event: " + event.type() + "\ndata: " + objectMapper.writeValueAsString(event.data()) + "\n\n")
                    .getBytes(StandardCharsets.UTF_8));
            output.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
