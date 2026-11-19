package com.educenter.controller;

import com.educenter.dto.ChatMessagePayload;
import com.educenter.entity.ChatMessage;
import com.educenter.entity.ChatMessageReaction;
import com.educenter.repository.ChatMessageReactionRepository;
import com.educenter.security.CustomUserDetails;
import com.educenter.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.List;

@Controller
@RequestMapping("/chat")
public class ChatController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatController.class);
    private final ChatService chatService;
    private final ChatMessageReactionRepository reactionRepository;

    public ChatController(ChatService chatService, ChatMessageReactionRepository reactionRepository) {
        this.chatService = chatService;
        this.reactionRepository = reactionRepository;
    }

    @GetMapping
    public String index(@AuthenticationPrincipal CustomUserDetails user,
                        @RequestParam(required = false) String search, Model model) {
        fillSharedModel(user, search, model);
        return "chat/index";
    }

    @PostMapping("/new")
    public String create(@AuthenticationPrincipal CustomUserDetails user,
                         @RequestParam(name = "userIds", required = false) List<Long> userIds,
                         @RequestParam(required = false) String title, RedirectAttributes redirect) {
        try {
            var conversation = chatService.createConversation(user.getId(), user.getRoleName(), userIds, title);
            return "redirect:/chat/" + conversation.getId();
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/chat";
        } catch (Exception e) {
            LOGGER.error("Could not create chat conversation for user {}", user.getId(), e);
            redirect.addFlashAttribute("errorMessage", "Không thể tạo hội thoại lúc này.");
            return "redirect:/chat";
        }
    }

    @GetMapping("/{conversationId}")
    public String open(@PathVariable Long conversationId, @AuthenticationPrincipal CustomUserDetails user,
                       @RequestParam(required = false) String search, Model model, RedirectAttributes redirect) {
        try {
            fillSharedModel(user, search, model);
            var chatPage = chatService.openConversation(conversationId, user.getId());
            model.addAttribute("chatPage", chatPage);
            java.util.Map<Long, List<ChatMessageReaction>> reactions = new java.util.HashMap<>();
            chatPage.messages().forEach(message -> reactions.put(message.getId(), reactionRepository.findByMessageId(message.getId())));
            model.addAttribute("reactionsByMessage", reactions);
            model.addAttribute("currentConversationId", conversationId);
            model.addAttribute("pollUrl", "/chat/" + conversationId + "/api/messages");
            model.addAttribute("sendUrl", "/chat/" + conversationId + "/messages");
            return "chat/thread";
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/chat";
        } catch (Exception e) {
            LOGGER.error("Could not open chat conversation {} for user {}", conversationId, user.getId(), e);
            redirect.addFlashAttribute("errorMessage", "Không thể mở hội thoại lúc này.");
            return "redirect:/chat";
        }
    }

    @PostMapping("/{conversationId}/messages")
    public String send(@PathVariable Long conversationId, @AuthenticationPrincipal CustomUserDetails user,
                       @RequestParam(required = false) String content,
                       @RequestParam(required = false) Long replyToId,
                       @RequestParam(required = false) MultipartFile file,
                       RedirectAttributes redirect) {
        try {
            chatService.send(conversationId, user.getId(), content, replyToId, file);
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Could not send chat message in conversation {}", conversationId, e);
            redirect.addFlashAttribute("errorMessage", "Không thể gửi tin nhắn lúc này.");
        }
        return "redirect:/chat/" + conversationId;
    }

    @GetMapping("/{conversationId}/api/messages")
    @ResponseBody
    public List<ChatMessagePayload> updates(@PathVariable Long conversationId,
                                            @RequestParam(defaultValue = "0") Long afterId,
                                            @AuthenticationPrincipal CustomUserDetails user) {
        return chatService.findMessagesAfter(conversationId, user.getId(), afterId);
    }

    @GetMapping("/{conversationId}/files/{messageId}")
    public ResponseEntity<Resource> file(@PathVariable Long conversationId, @PathVariable Long messageId,
                                         @AuthenticationPrincipal CustomUserDetails user) {
        ChatMessage message = chatService.findMessageForMember(conversationId, messageId, user.getId());
        Resource resource = new FileSystemResource(chatService.resolveFile(message));
        MediaType type = MediaTypeFactory.getMediaType(message.getOriginalFilename()).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok().contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(message.getOriginalFilename(), StandardCharsets.UTF_8).build().toString())
                .body(resource);
    }

    @GetMapping("/{conversationId}/avatar")
    public ResponseEntity<Resource> avatar(@PathVariable Long conversationId,
                                            @AuthenticationPrincipal CustomUserDetails user) {
        Resource resource = new FileSystemResource(chatService.resolveAvatar(conversationId, user.getId()));
        MediaType type = MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok().contentType(type).body(resource);
    }

    @PostMapping("/{conversationId}/avatar")
    public String uploadAvatar(@PathVariable Long conversationId,
                               @RequestParam MultipartFile avatar,
                               @AuthenticationPrincipal CustomUserDetails user,
                               RedirectAttributes redirect) {
        try { chatService.setAvatar(conversationId, user.getId(), avatar); }
        catch (IllegalArgumentException e) { redirect.addFlashAttribute("errorMessage", e.getMessage()); }
        return "redirect:/chat/" + conversationId;
    }

    @PostMapping("/{conversationId}/messages/{messageId}/reaction")
    public String react(@PathVariable Long conversationId, @PathVariable Long messageId,
                        @RequestParam String emoji, @AuthenticationPrincipal CustomUserDetails user,
                        RedirectAttributes redirect) {
        try { chatService.toggleReaction(conversationId, messageId, user.getId(), emoji); }
        catch (IllegalArgumentException e) { redirect.addFlashAttribute("errorMessage", e.getMessage()); }
        return "redirect:/chat/" + conversationId;
    }

    @PostMapping("/{conversationId}/messages/{messageId}/delete")
    public String delete(@PathVariable Long conversationId, @PathVariable Long messageId,
                         @AuthenticationPrincipal CustomUserDetails user, RedirectAttributes redirect) {
        try { chatService.deleteForAll(conversationId, messageId, user.getId()); }
        catch (IllegalArgumentException e) { redirect.addFlashAttribute("errorMessage", e.getMessage()); }
        return "redirect:/chat/" + conversationId;
    }

    @PostMapping("/{conversationId}/messages/{messageId}/forward")
    public String forward(@PathVariable Long conversationId, @PathVariable Long messageId,
                          @RequestParam Long targetConversationId,
                          @AuthenticationPrincipal CustomUserDetails user, RedirectAttributes redirect) {
        try {
            chatService.forward(conversationId, messageId, targetConversationId, user.getId());
            redirect.addFlashAttribute("successMessage", "Đã chuyển tiếp tin nhắn.");
            return "redirect:/chat/" + targetConversationId;
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/chat/" + conversationId;
        }
    }

    @PostMapping("/{conversationId}/rename")
    public String rename(@PathVariable Long conversationId, @RequestParam String title,
                         @AuthenticationPrincipal CustomUserDetails user, RedirectAttributes redirect) {
        try { chatService.rename(conversationId, user.getId(), title); }
        catch (IllegalArgumentException e) { redirect.addFlashAttribute("errorMessage", e.getMessage()); }
        return "redirect:/chat/" + conversationId;
    }

    @PostMapping("/{conversationId}/members")
    public String addMembers(@PathVariable Long conversationId,
                             @RequestParam(name = "userIds", required = false) List<Long> userIds,
                             @AuthenticationPrincipal CustomUserDetails user, RedirectAttributes redirect) {
        try { chatService.addMembers(conversationId, user.getId(), userIds); }
        catch (IllegalArgumentException e) { redirect.addFlashAttribute("errorMessage", e.getMessage()); }
        return "redirect:/chat/" + conversationId;
    }

    @PostMapping("/{conversationId}/members/{memberId}/remove")
    public String removeMember(@PathVariable Long conversationId, @PathVariable Long memberId,
                               @AuthenticationPrincipal CustomUserDetails user, RedirectAttributes redirect) {
        try { chatService.removeMember(conversationId, user.getId(), memberId); }
        catch (IllegalArgumentException e) { redirect.addFlashAttribute("errorMessage", e.getMessage()); }
        return "redirect:/chat/" + conversationId;
    }

    @PostMapping("/{conversationId}/archive")
    public String archive(@PathVariable Long conversationId, @AuthenticationPrincipal CustomUserDetails user) {
        chatService.archive(conversationId, user.getId());
        return "redirect:/chat";
    }

    @PostMapping("/{conversationId}/notifications")
    public String notifications(@PathVariable Long conversationId, @RequestParam boolean enabled,
                                @AuthenticationPrincipal CustomUserDetails user) {
        chatService.setNotifications(conversationId, user.getId(), enabled);
        return "redirect:/chat/" + conversationId;
    }

    private void fillSharedModel(CustomUserDetails user, String search, Model model) {
        model.addAttribute("currentUser", user);
        model.addAttribute("search", search == null ? "" : search);
        model.addAttribute("conversations", chatService.findConversations(user.getId(), search));
        model.addAttribute("contacts", chatService.findContacts(user.getId(), user.getRoleName()));
        model.addAttribute("reactionChoices", List.of("👍", "❤️", "😂", "😮", "😢", "🙏", "🎉"));
        model.addAttribute("createLabel", switch (user.getRoleName()) {
            case "ROLE_PARENT" -> "Tạo nhóm phụ huynh";
            case "ROLE_TEACHER" -> "Tạo nhóm người dạy";
            default -> "Tạo hội thoại mới";
        });
    }
}
