package com.educenter.controller.admin;

import com.educenter.entity.ClassEntity;
import com.educenter.entity.Student;
import com.educenter.entity.Tuition;
import com.educenter.service.ClassService;
import com.educenter.service.StudentService;
import com.educenter.service.TuitionService;
import com.educenter.service.ChatService;
import com.educenter.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/tuition")
public class AdminTuitionController {

    private final TuitionService tuitionService;
    private final StudentService studentService;
    private final ClassService classService;
    private final ChatService chatService;

    public AdminTuitionController(TuitionService tuitionService,
                                  StudentService studentService,
                                  ClassService classService,
                                  ChatService chatService) {
        this.tuitionService = tuitionService;
        this.studentService = studentService;
        this.classService = classService;
        this.chatService = chatService;
    }

    @GetMapping
    public String listTuition(Model model) {
        List<Tuition> tuitions = tuitionService.getAllTuition();
        BigDecimal totalPaid = tuitionService.getTotalPaidRevenue();
        long unpaidCount = tuitionService.getUnpaidCount();
        long paidCount = tuitionService.getPaidCount();

        model.addAttribute("tuitions", tuitions);
        model.addAttribute("totalPaid", totalPaid);
        model.addAttribute("unpaidCount", unpaidCount);
        model.addAttribute("paidCount", paidCount);
        return "admin/tuition/list";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("students", studentService.findAllActive());
        model.addAttribute("classes", classService.getActiveClasses());
        return "admin/tuition/form";
    }

    @PostMapping("/add")
    public String addTuition(@RequestParam Long studentId,
                             @RequestParam Long classId,
                             @RequestParam BigDecimal amount,
                             @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dueDate,
                             @RequestParam(required = false) String description,
                             RedirectAttributes redirectAttributes) {
        try {
            tuitionService.createTuition(studentId, classId, amount, dueDate, description);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo khoản thu học phí thành công!");
            return "redirect:/admin/tuition";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/tuition/add";
        }
    }

    @PostMapping("/mark-paid/{id}")
    public String markAsPaid(@PathVariable Long id,
                             @RequestParam(defaultValue = "CASH") String paymentMethod,
                             @RequestParam(required = false) String note,
                             RedirectAttributes redirectAttributes) {
        try {
            tuitionService.markAsPaid(id, paymentMethod, "ADMIN_CONFIRM_" + System.currentTimeMillis(), note);
            redirectAttributes.addFlashAttribute("successMessage", "Xác nhận đóng học phí thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/tuition";
    }

    @PostMapping("/assign-period/{id}")
    public String assignPeriod(@PathVariable Long id, @RequestParam String month, RedirectAttributes redirectAttributes) {
        try {
            tuitionService.assignBillingPeriod(id, YearMonth.parse(month));
            redirectAttributes.addFlashAttribute("successMessage", "Đã gắn khoản thu cũ vào kỳ " + month + ".");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/tuition";
    }

    @PostMapping("/notify-parent/{parentUserId}")
    public String notifyParent(@PathVariable Long parentUserId,
                               @AuthenticationPrincipal CustomUserDetails admin,
                               RedirectAttributes redirectAttributes) {
        try {
            String reminder = buildReminder(tuitionService.getTuitionByParentUserId(parentUserId));
            var conversation = chatService.sendAdminMessageToParent(admin.getId(), parentUserId, reminder);
            redirectAttributes.addFlashAttribute("successMessage", "Đã gửi thông báo học phí gộp các con cho phụ huynh.");
            return "redirect:/chat/" + conversation.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/tuition";
        }
    }

    @PostMapping("/notify-all")
    public String notifyAllParents(@AuthenticationPrincipal CustomUserDetails admin,
                                   RedirectAttributes redirectAttributes) {
        Map<Long, List<Tuition>> byParent = tuitionService.getAllTuition().stream()
                .filter(tuition -> "UNPAID".equals(tuition.getStatus()))
                .collect(Collectors.groupingBy(tuition -> tuition.getStudent().getParent().getUser().getId()));
        int sent = 0;
        int failed = 0;
        for (Map.Entry<Long, List<Tuition>> entry : byParent.entrySet()) {
            try {
                chatService.sendAdminMessageToParent(admin.getId(), entry.getKey(), buildReminder(entry.getValue()));
                sent++;
            } catch (Exception e) {
                failed++;
            }
        }
        if (sent == 0 && failed == 0) redirectAttributes.addFlashAttribute("successMessage", "Hiện không có khoản học phí chưa thanh toán.");
        else redirectAttributes.addFlashAttribute("successMessage", "Đã gửi thông báo tới " + sent + " phụ huynh." + (failed > 0 ? " Có " + failed + " phụ huynh chưa gửi được." : ""));
        return "redirect:/admin/tuition";
    }

    private String buildReminder(List<Tuition> parentTuitions) {
        List<Tuition> unpaid = parentTuitions.stream().filter(tuition -> "UNPAID".equals(tuition.getStatus())).toList();
        if (unpaid.isEmpty()) throw new IllegalArgumentException("Phụ huynh này không có khoản học phí chưa thanh toán.");
        BigDecimal total = unpaid.stream().map(Tuition::getAmount).filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        StringBuilder message = new StringBuilder("Thông báo học phí chưa thanh toán\n");
        for (Tuition tuition : unpaid) {
            message.append("• ").append(tuition.getStudent().getFullName()).append(" — ")
                    .append(tuition.getClassEntity().getName()).append(": ")
                    .append(tuition.getAmount().stripTrailingZeros().toPlainString()).append(" đ");
            if (tuition.getDueDate() != null) message.append(" (hạn ").append(tuition.getDueDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append(")");
            message.append('\n');
        }
        message.append("Tổng cần thanh toán: ").append(total.stripTrailingZeros().toPlainString()).append(" đ. Vui lòng mở mục Học phí để xem và thanh toán.");
        return message.toString();
    }
}
