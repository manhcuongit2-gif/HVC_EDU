package com.educenter.controller.parent;

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
@RequestMapping("/parent/messages")
public class ParentMessageController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParentMessageController.class);

    private final ParentTeacherMessageService messageService;

    public ParentMessageController(ParentTeacherMessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    public String index(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        model.addAttribute("currentUser", user);
        model.addAttribute("classes", messageService.findParentClasses(user.getId()));
        return "parent/messages";
    }

    @GetMapping("/{classId}")
    public String conversation(@PathVariable Long classId, @AuthenticationPrincipal CustomUserDetails user,
                              Model model, RedirectAttributes redirect) {
        try {
            ConversationPage page = messageService.openForParent(user.getId(), classId);
            model.addAttribute("currentUser", user);
            model.addAttribute("conversation", page.conversation());
            model.addAttribute("messages", page.messages());
            model.addAttribute("title", page.conversation().getClassEntity().getName());
            model.addAttribute("postUrl", "/parent/messages/" + classId);
            model.addAttribute("backUrl", "/parent/messages");
            model.addAttribute("backLabel", "Danh sách hội thoại");
            model.addAttribute("canSend", true);
            return "messages/thread";
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Could not open parent to teacher conversation for class {}", classId, e);
            redirect.addFlashAttribute("errorMessage", "Không thể mở hội thoại lúc này. Vui lòng thử lại sau.");
            return "redirect:/parent/messages";
        }
        return "redirect:/parent/messages";
    }

    @PostMapping("/{classId}")
    public String send(@PathVariable Long classId, @RequestParam String content,
                       @AuthenticationPrincipal CustomUserDetails user, RedirectAttributes redirect) {
        try {
            messageService.sendAsParent(user.getId(), classId, content);
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Could not save parent to teacher message for class {}", classId, e);
            redirect.addFlashAttribute("errorMessage", "Không thể gửi tin nhắn lúc này. Vui lòng thử lại sau.");
        }
        return "redirect:/parent/messages/" + classId;
    }
}
