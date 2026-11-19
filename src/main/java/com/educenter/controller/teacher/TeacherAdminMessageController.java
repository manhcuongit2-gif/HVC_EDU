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
            model.addAttribute("messages", page.messages());
            model.addAttribute("title", "Trao đổi với quản trị viên");
            model.addAttribute("postUrl", "/teacher/admin-messages");
            model.addAttribute("backUrl", "/teacher/dashboard");
            model.addAttribute("backLabel", "Về dashboard");
            model.addAttribute("canSend", true);
            return "messages/thread";
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Could not open teacher to admin conversation for user {}", user.getId(), e);
            redirect.addFlashAttribute("errorMessage", "Không thể mở hội thoại lúc này. Vui lòng thử lại sau.");
        }
        return "redirect:/teacher/dashboard";
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
