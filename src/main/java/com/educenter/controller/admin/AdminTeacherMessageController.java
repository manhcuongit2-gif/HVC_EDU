package com.educenter.controller.admin;

import com.educenter.dto.TeacherAdminConversationPage;
import com.educenter.security.CustomUserDetails;
import com.educenter.service.TeacherAdminMessageService;
import org.springframework.dao.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/admin/messages")
public class AdminTeacherMessageController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminTeacherMessageController.class);

    private final TeacherAdminMessageService messageService;

    public AdminTeacherMessageController(TeacherAdminMessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    public String index() {
        return "redirect:/chat?tab=teacher";
    }

    @GetMapping("/{teacherUserId}")
    public String conversation(@PathVariable Long teacherUserId, @AuthenticationPrincipal CustomUserDetails user,
                              Model model, RedirectAttributes redirect) {
        try {
            TeacherAdminConversationPage page = messageService.openForAdmin(user.getId(), teacherUserId);
            model.addAttribute("currentUser", user);
            model.addAttribute("conversation", page.conversation());
            model.addAttribute("liveTopic", "/topic/teacher-admin/" + page.conversation().getId());
            model.addAttribute("pollUrl", "/admin/messages/" + teacherUserId + "/api/messages");
            model.addAttribute("messages", page.messages());
            model.addAttribute("title", "Trao đổi với " + page.conversation().getTeacherUser().getFullName());
            model.addAttribute("postUrl", "/admin/messages/" + teacherUserId);
            model.addAttribute("backUrl", "/chat?tab=teacher");
            model.addAttribute("backLabel", "Hộp thư");
            model.addAttribute("canSend", true);
            return "messages/thread";
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Could not open admin to teacher conversation for teacher user {}", teacherUserId, e);
            redirect.addFlashAttribute("errorMessage", "Không thể mở hội thoại lúc này. Vui lòng thử lại sau.");
        }
        return "redirect:/chat?tab=teacher";
    }

    @GetMapping(value = "/{teacherUserId}/api/messages", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public java.util.List<com.educenter.dto.LegacyMessagePayload> updates(@PathVariable Long teacherUserId,
            @RequestParam(defaultValue = "0") Long afterId, @AuthenticationPrincipal CustomUserDetails user) {
        try { return messageService.findMessagesAfterForAdmin(user.getId(), teacherUserId, afterId); }
        catch (IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e); }
    }

    @PostMapping("/{teacherUserId}")
    public String send(@PathVariable Long teacherUserId, @RequestParam String content,
                       @AuthenticationPrincipal CustomUserDetails user, RedirectAttributes redirect) {
        try {
            messageService.sendAsAdmin(user.getId(), teacherUserId, content);
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Could not save admin to teacher message for teacher user {}", teacherUserId, e);
            redirect.addFlashAttribute("errorMessage", "Không thể gửi tin nhắn lúc này. Vui lòng thử lại sau.");
        }
        return "redirect:/admin/messages/" + teacherUserId;
    }
}
