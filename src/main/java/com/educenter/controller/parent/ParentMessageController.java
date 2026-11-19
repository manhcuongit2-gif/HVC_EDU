package com.educenter.controller.parent;

import com.educenter.dto.ConversationPage;
import com.educenter.entity.ClassEntity;
import com.educenter.security.CustomUserDetails;
import com.educenter.service.ParentTeacherMessageService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/parent/messages")
public class ParentMessageController {

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
            return "messages/thread";
        } catch (Exception e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/parent/messages";
        }
    }

    @PostMapping("/{classId}")
    public String send(@PathVariable Long classId, @RequestParam String content,
                       @AuthenticationPrincipal CustomUserDetails user, RedirectAttributes redirect) {
        try {
            messageService.sendAsParent(user.getId(), classId, content);
        } catch (Exception e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/parent/messages/" + classId;
    }
}
