package com.educenter.controller.admin;

import com.educenter.entity.ClassEntity;
import com.educenter.entity.Student;
import com.educenter.entity.Tuition;
import com.educenter.service.ClassService;
import com.educenter.service.StudentService;
import com.educenter.service.TuitionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Controller
@RequestMapping("/admin/tuition")
public class AdminTuitionController {

    private final TuitionService tuitionService;
    private final StudentService studentService;
    private final ClassService classService;

    public AdminTuitionController(TuitionService tuitionService,
                                  StudentService studentService,
                                  ClassService classService) {
        this.tuitionService = tuitionService;
        this.studentService = studentService;
        this.classService = classService;
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
}
