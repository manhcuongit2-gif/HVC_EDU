package com.educenter.controller.teacher;

import com.educenter.dto.TeacherEarningsPage;
import com.educenter.security.CustomUserDetails;
import com.educenter.service.TeacherPayService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.YearMonth;
import java.time.ZoneId;

@Controller
@RequestMapping("/teacher/earnings")
public class TeacherEarningsController {

    private final TeacherPayService teacherPayService;

    public TeacherEarningsController(TeacherPayService teacherPayService) {
        this.teacherPayService = teacherPayService;
    }

    @GetMapping
    public String index(@AuthenticationPrincipal CustomUserDetails user,
                        @RequestParam(required = false) String month,
                        Model model) {
        YearMonth selectedMonth = parseMonth(month);
        TeacherEarningsPage page = teacherPayService.getEarnings(user.getId(), selectedMonth);
        model.addAttribute("currentUser", user);
        model.addAttribute("earnings", page);
        model.addAttribute("previousMonth", selectedMonth.minusMonths(1));
        model.addAttribute("nextMonth", selectedMonth.plusMonths(1));
        return "teacher/earnings";
    }

    private YearMonth parseMonth(String value) {
        try {
            return value == null || value.isBlank() ? YearMonth.now(ZoneId.of("Asia/Ho_Chi_Minh")) : YearMonth.parse(value);
        } catch (RuntimeException e) {
            return YearMonth.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        }
    }
}
