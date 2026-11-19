package com.educenter.controller.admin;

import com.educenter.entity.Registration;
import com.educenter.entity.Tuition;
import com.educenter.repository.*;
import com.educenter.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    private final SubjectRepository subjectRepository;
    private final RoomRepository roomRepository;
    private final TeacherRepository teacherRepository;
    private final ParentRepository parentRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final ClassRepository classRepository;
    private final TuitionRepository tuitionRepository;
    private final RegistrationRepository registrationRepository;
    private final ScheduleRepository scheduleRepository;
    private final ClassSessionRepository classSessionRepository;

    public AdminDashboardController(SubjectRepository subjectRepository,
                                    RoomRepository roomRepository,
                                    TeacherRepository teacherRepository,
                                    ParentRepository parentRepository,
                                    StudentRepository studentRepository,
                                    UserRepository userRepository,
                                    ClassRepository classRepository,
                                    TuitionRepository tuitionRepository,
                                    RegistrationRepository registrationRepository,
                                    ScheduleRepository scheduleRepository,
                                    ClassSessionRepository classSessionRepository) {
        this.subjectRepository = subjectRepository;
        this.roomRepository = roomRepository;
        this.teacherRepository = teacherRepository;
        this.parentRepository = parentRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.classRepository = classRepository;
        this.tuitionRepository = tuitionRepository;
        this.registrationRepository = registrationRepository;
        this.scheduleRepository = scheduleRepository;
        this.classSessionRepository = classSessionRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        BigDecimal totalPaid = tuitionRepository.calculateTotalPaidRevenue();
        List<Registration> pendingRegistrations = registrationRepository.findByStatusOrderByRegistrationDateDesc("PENDING");

        model.addAttribute("currentUser", userDetails);
        model.addAttribute("subjectCount", subjectRepository.count());
        model.addAttribute("roomCount", roomRepository.count());
        model.addAttribute("teacherCount", teacherRepository.count());
        model.addAttribute("parentCount", parentRepository.count());
        model.addAttribute("studentCount", studentRepository.count());
        model.addAttribute("classCount", classRepository.count());
        model.addAttribute("totalRevenue", totalPaid != null ? totalPaid : BigDecimal.ZERO);
        model.addAttribute("pendingRegistrationCount", pendingRegistrations.size());
        model.addAttribute("recentPendingRegistrations", pendingRegistrations);
        model.addAttribute("scheduleCount", scheduleRepository.count());
        return "admin/dashboard";
    }

    @GetMapping("/statistics")
    public String statistics(@AuthenticationPrincipal CustomUserDetails userDetails,
                             @org.springframework.web.bind.annotation.RequestParam(required = false) String period,
                             @org.springframework.web.bind.annotation.RequestParam(required = false) String month,
                             @org.springframework.web.bind.annotation.RequestParam(required = false) Integer year,
                             Model model) {
        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        YearMonth selectedMonth = parseMonth(month, YearMonth.from(LocalDate.now(zone)));
        int selectedYear = year == null ? selectedMonth.getYear() : Math.max(2000, Math.min(2100, year));
        boolean annual = "year".equalsIgnoreCase(period);
        LocalDate fromDate = annual ? LocalDate.of(selectedYear, 1, 1) : selectedMonth.atDay(1);
        LocalDate toDate = annual ? LocalDate.of(selectedYear, 12, 31) : selectedMonth.atEndOfMonth();
        LocalDate previousFromDate = annual ? fromDate.minusYears(1) : selectedMonth.minusMonths(1).atDay(1);
        LocalDate previousToDate = annual ? toDate.minusYears(1) : selectedMonth.minusMonths(1).atEndOfMonth();
        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime toExclusive = toDate.plusDays(1).atStartOfDay();
        LocalDateTime previousFrom = previousFromDate.atStartOfDay();
        LocalDateTime previousToExclusive = previousToDate.plusDays(1).atStartOfDay();

        BigDecimal totalPaid = tuitionRepository.calculateTotalPaidRevenue();
        long unpaidCount = tuitionRepository.countByStatus("UNPAID");
        long paidCount = tuitionRepository.countByStatus("PAID");
        List<Tuition> allTuitions = tuitionRepository.findAll();
        List<Tuition> periodTuitions = allTuitions.stream().filter(t -> isInRange(t, fromDate, toDate)).toList();
        BigDecimal periodDue = sum(periodTuitions);
        BigDecimal periodPaid = sum(periodTuitions.stream().filter(t -> "PAID".equals(t.getStatus())).toList());
        BigDecimal periodUnpaid = sum(periodTuitions.stream().filter(t -> "UNPAID".equals(t.getStatus())).toList());
        long newRegistrationCount = registrationRepository.countByRegistrationDateBetween(from, toExclusive.minusNanos(1));
        long previousRegistrationCount = registrationRepository.countByRegistrationDateBetween(previousFrom, previousToExclusive.minusNanos(1));
        long newStudentCount = studentRepository.countByCreatedAtBetween(from, toExclusive.minusNanos(1));
        List<com.educenter.entity.ClassSession> sessions = classSessionRepository.findAllReportableBetween(fromDate, toDate);
        long reportedSessionCount = sessions.stream().filter(session -> session.getPublishedAt() != null).count();
        long pastUnreportedCount = sessions.stream().filter(session -> session.getPublishedAt() == null && session.getSessionDate().isBefore(LocalDate.now(zone))).count();
        List<String> monthlyLabels = new ArrayList<>();
        List<Double> monthlyPaid = new ArrayList<>();
        List<Double> monthlyUnpaid = new ArrayList<>();
        for (int monthIndex = 1; monthIndex <= 12; monthIndex++) {
            YearMonth plottedMonth = YearMonth.of(selectedYear, monthIndex);
            List<Tuition> values = allTuitions.stream().filter(t -> isInRange(t, plottedMonth.atDay(1), plottedMonth.atEndOfMonth())).toList();
            monthlyLabels.add(String.format("%02d", monthIndex));
            monthlyPaid.add(sum(values.stream().filter(t -> "PAID".equals(t.getStatus())).toList()).doubleValue());
            monthlyUnpaid.add(sum(values.stream().filter(t -> "UNPAID".equals(t.getStatus())).toList()).doubleValue());
        }

        model.addAttribute("currentUser", userDetails);
        model.addAttribute("studentCount", studentRepository.count());
        model.addAttribute("teacherCount", teacherRepository.count());
        model.addAttribute("classCount", classRepository.count());
        model.addAttribute("subjectCount", subjectRepository.count());
        model.addAttribute("totalRevenue", totalPaid != null ? totalPaid : BigDecimal.ZERO);
        model.addAttribute("unpaidCount", unpaidCount);
        model.addAttribute("paidCount", paidCount);
        model.addAttribute("period", annual ? "year" : "month");
        model.addAttribute("selectedMonth", selectedMonth.toString());
        model.addAttribute("selectedYear", selectedYear);
        model.addAttribute("periodLabel", annual ? String.valueOf(selectedYear) : selectedMonth.toString());
        model.addAttribute("periodDue", periodDue);
        model.addAttribute("periodPaid", periodPaid);
        model.addAttribute("periodUnpaid", periodUnpaid);
        model.addAttribute("newRegistrationCount", newRegistrationCount);
        model.addAttribute("previousRegistrationCount", previousRegistrationCount);
        model.addAttribute("newStudentCount", newStudentCount);
        model.addAttribute("sessionCount", sessions.size());
        model.addAttribute("reportedSessionCount", reportedSessionCount);
        model.addAttribute("pastUnreportedCount", pastUnreportedCount);
        model.addAttribute("reportCompletionPercent", sessions.isEmpty() ? 0 : Math.round(reportedSessionCount * 100.0 / sessions.size()));
        model.addAttribute("monthlyLabels", monthlyLabels);
        model.addAttribute("monthlyPaid", monthlyPaid);
        model.addAttribute("monthlyUnpaid", monthlyUnpaid);
        return "admin/statistics";
    }

    private YearMonth parseMonth(String value, YearMonth fallback) {
        if (value != null) try { return YearMonth.parse(value); } catch (DateTimeParseException ignored) { }
        return fallback;
    }

    private boolean isInRange(Tuition tuition, LocalDate from, LocalDate to) {
        LocalDate period = tuition.getBillingPeriod() != null ? tuition.getBillingPeriod() : tuition.getDueDate();
        return period != null && !period.isBefore(from) && !period.isAfter(to);
    }

    private BigDecimal sum(List<Tuition> tuitions) {
        return tuitions.stream().map(Tuition::getAmount).filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
