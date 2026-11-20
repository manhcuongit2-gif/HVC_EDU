package com.educenter.service.impl;

import com.educenter.dto.ChatPage;
import com.educenter.entity.ChatMessage;
import com.educenter.service.AIChatAssistService;
import com.educenter.service.AIContextService;
import com.educenter.service.AIService;
import com.educenter.service.ChatService;
import org.springframework.stereotype.Service;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AIChatAssistServiceImpl implements AIChatAssistService {
    private static final Set<String> ACTIONS = Set.of("SUGGEST_REPLY", "PROOFREAD", "PROFESSIONAL", "SHORTEN", "EXPAND", "PARENT_NOTICE");
    private final ChatService chatService;
    private final AIService aiService;
    private final AIContextService contextService;

    public AIChatAssistServiceImpl(ChatService chatService, AIService aiService, AIContextService contextService) {
        this.chatService = chatService;
        this.aiService = aiService;
        this.contextService = contextService;
    }

    @Override
    public String createDraft(Long userId, String role, Long conversationId, String action, String draft) {
        String operation = action == null ? "" : action.trim().toUpperCase(Locale.ROOT);
        if (!ACTIONS.contains(operation)) throw new IllegalArgumentException("Công cụ soạn tin nhắn không hợp lệ.");
        String cleanDraft = draft == null ? "" : draft.trim();
        if (cleanDraft.length() > 2000) throw new IllegalArgumentException("Bản nháp không được vượt quá 2.000 ký tự.");
        ChatPage page = chatService.openConversation(conversationId, userId);
        String history = page.messages().stream().filter(m -> !m.isDeletedForAll())
                .sorted(Comparator.comparing(ChatMessage::getId)).skip(Math.max(0, page.messages().size() - 12L))
                .map(m -> (m.getSender().getId().equals(userId) ? "Người dùng: " : "Người trao đổi: ")
                        + (m.getContent() == null || m.getContent().isBlank() ? "[tin nhắn có tệp đính kèm]" : m.getContent()))
                .collect(Collectors.joining("\n"));
        for (var member : page.members()) {
            String name = member.getUser().getFullName();
            if (name != null && name.length() >= 3) {
                history = Pattern.compile(Pattern.quote(name), Pattern.CASE_INSENSITIVE).matcher(history)
                        .replaceAll("[người tham gia]");
                cleanDraft = Pattern.compile(Pattern.quote(name), Pattern.CASE_INSENSITIVE).matcher(cleanDraft)
                        .replaceAll("[người tham gia]");
            }
        }
        history = contextService.sanitizeText(userId, role, null, history);
        cleanDraft = contextService.sanitizeText(userId, role, null, cleanDraft);
        String system = "Bạn là trợ lý soạn thảo tin nhắn cho người dùng trong hệ thống giáo dục. "
                + "Chỉ soạn bản nháp bằng tiếng Việt, giữ đúng ý và giọng điệu phù hợp. "
                + "Không gửi tin nhắn, không thêm thông tin cá nhân hay sự kiện không có trong nội dung. "
                + "Trả về duy nhất bản nháp, tối đa 2.000 ký tự.";
        String prompt = switch (operation) {
            case "SUGGEST_REPLY" -> "Soạn câu trả lời phù hợp cho cuộc trao đổi sau. Người dùng sẽ tự kiểm tra và gửi.\n" + history
                    + (cleanDraft.isBlank() ? "" : "\nÝ muốn bổ sung: " + cleanDraft);
            case "PROOFREAD" -> "Sửa chính tả và dấu câu, không đổi ý: " + cleanDraft;
            case "PROFESSIONAL" -> "Viết lại lịch sự, chuyên nghiệp và dễ hiểu, giữ nguyên thông tin: " + cleanDraft;
            case "SHORTEN" -> "Rút gọn, giữ các ý chính: " + cleanDraft;
            case "EXPAND" -> "Làm rõ và bổ sung diễn đạt nhưng không thêm sự kiện mới: " + cleanDraft;
            case "PARENT_NOTICE" -> "Soạn một tin nhắn ngắn, lịch sự gửi phụ huynh dựa trên nội dung sau: " + cleanDraft;
            default -> throw new IllegalArgumentException("Công cụ soạn tin nhắn không hợp lệ.");
        };
        if (cleanDraft.isBlank() && !"SUGGEST_REPLY".equals(operation))
            throw new IllegalArgumentException("Nhập nội dung cần chỉnh sửa trước.");
        return aiService.generateText(userId, role, system, prompt).trim();
    }
}
