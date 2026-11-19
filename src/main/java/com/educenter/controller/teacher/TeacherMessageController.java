package com.educenter.controller.teacher;

import com.educenter.dto.ConversationPage;
import com.educenter.security.CustomUserDetails;
import com.educenter.service.ParentTeacherMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/teacher/messages")
public class TeacherMessageController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TeacherMessageController.class);

    private final ParentTeacherMessageService messageService;

    public TeacherMessageController(ParentTeacherMessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    public String index(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        model.addAttribute("currentUser", user);
        model.addAttribute("targets", messageService.findTeacherTargets(user.getId()));
        return "teacher/messages";
    }

    @GetMapping("/{classId}/{parentId}")
    public String conversation(@PathVariable Long classId, @PathVariable Long parentId,
                              @AuthenticationPrincipal CustomUserDetails user, Model model,
                              RedirectAttributes redirect) {
        try {
            ConversationPage page = messageService.openForTeacher(user.getId(), classId, parentId);
            model.addAttribute("currentUser", user);
            model.addAttribute("conversation", page.conversation());
            model.addAttribute("messages", page.messages());
            model.addAttribute("title", page.conversation().getParent().getUser().getFullName() + " · "
                    + page.conversation().getClassEntity().getName());
            model.addAttribute("postUrl", "/teacher/messages/" + classId + "/" + parentId);
            model.addAttribute("backUrl", "/teacher/messages");
            model.addAttribute("backLabel", "Danh sách hội thoại");
            model.addAttribute("canSend", true);
            return "messages/thread";
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Could not open teacher to parent conversation for class {} and parent {}", classId, parentId, e);
            redirect.addFlashAttribute("errorMessage", "Không thể mở hội thoại lúc này. Vui lòng thử lại sau.");
            return "redirect:/teacher/messages";
        }
        return "redirect:/teacher/messages";
    }

    @PostMapping("/{classId}/{parentId}")
    public String send(@PathVariable Long classId, @PathVariable Long parentId, @RequestParam String content,
                       @AuthenticationPrincipal CustomUserDetails user, RedirectAttributes redirect) {
        try {
            messageService.sendAsTeacher(user.getId(), classId, parentId, content);
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Could not save teacher to parent message for class {} and parent {}", classId, parentId, e);
            redirect.addFlashAttribute("errorMessage", "Không thể gửi tin nhắn lúc này. Vui lòng thử lại sau.");
        }
        return "redirect:/teacher/messages/" + classId + "/" + parentId;
    }
}
