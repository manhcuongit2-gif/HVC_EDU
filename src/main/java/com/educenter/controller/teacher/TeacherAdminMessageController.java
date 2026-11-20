package com.educenter.controller.teacher;

import com.educenter.dto.TeacherAdminConversationPage;
import com.educenter.security.CustomUserDetails;
import com.educenter.service.TeacherAdminMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/teacher/admin-messages")
public class TeacherAdminMessageController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TeacherAdminMessageController.class);

    private final TeacherAdminMessageService messageService;

    public TeacherAdminMessageController(TeacherAdminMessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    public String conversation(@AuthenticationPrincipal CustomUserDetails user, Model model,
                               RedirectAttributes redirect) {
        try {
            TeacherAdminConversationPage page = messageService.openForTeacher(user.getId());
            model.addAttribute("currentUser", user);
            model.addAttribute("conversation", page.conversation());
            model.addAttribute("liveTopic", "/topic/teacher-admin/" + page.conversation().getId());
            model.addAttribute("pollUrl", "/teacher/admin-messages/api/messages");
            model.addAttribute("messages", page.messages());
            model.addAttribute("title", "Trao đổi với quản trị viên");
            model.addAttribute("postUrl", "/teacher/admin-messages");
            model.addAttribute("backUrl", "/chat?tab=admin");
            model.addAttribute("backLabel", "Hộp thư");
            model.addAttribute("canSend", true);
            return "messages/thread";
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Could not open teacher to admin conversation for user {}", user.getId(), e);
            redirect.addFlashAttribute("errorMessage", "Không thể mở hội thoại lúc này. Vui lòng thử lại sau.");
        }
        return "redirect:/chat?tab=admin";
    }

    @GetMapping(value = "/api/messages", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public java.util.List<com.educenter.dto.LegacyMessagePayload> updates(@RequestParam(defaultValue = "0") Long afterId,
            @AuthenticationPrincipal CustomUserDetails user) {
        try { return messageService.findMessagesAfterForTeacher(user.getId(), afterId); }
        catch (IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e); }
    }

    @PostMapping
    public String send(@AuthenticationPrincipal CustomUserDetails user, @RequestParam String content,
                       RedirectAttributes redirect) {
        try {
            messageService.sendAsTeacher(user.getId(), content);
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Could not save teacher to admin message for user {}", user.getId(), e);
            redirect.addFlashAttribute("errorMessage", "Không thể gửi tin nhắn lúc này. Vui lòng thử lại sau.");
        }
        return "redirect:/teacher/admin-messages";
    }
}
