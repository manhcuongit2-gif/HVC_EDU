package com.educenter.controller.admin;

import com.educenter.entity.Registration;
import com.educenter.repository.*;
import com.educenter.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
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

    public AdminDashboardController(SubjectRepository subjectRepository,
                                    RoomRepository roomRepository,
                                    TeacherRepository teacherRepository,
                                    ParentRepository parentRepository,
                                    StudentRepository studentRepository,
                                    UserRepository userRepository,
                                    ClassRepository classRepository,
                                    TuitionRepository tuitionRepository,
                                    RegistrationRepository registrationRepository,
                                    ScheduleRepository scheduleRepository) {
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
    public String statistics(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        BigDecimal totalPaid = tuitionRepository.calculateTotalPaidRevenue();
        long unpaidCount = tuitionRepository.countByStatus("UNPAID");
        long paidCount = tuitionRepository.countByStatus("PAID");

        model.addAttribute("currentUser", userDetails);
        model.addAttribute("studentCount", studentRepository.count());
        model.addAttribute("teacherCount", teacherRepository.count());
        model.addAttribute("classCount", classRepository.count());
        model.addAttribute("subjectCount", subjectRepository.count());
        model.addAttribute("totalRevenue", totalPaid != null ? totalPaid : BigDecimal.ZERO);
        model.addAttribute("unpaidCount", unpaidCount);
        model.addAttribute("paidCount", paidCount);
        return "admin/statistics";
    }
}
