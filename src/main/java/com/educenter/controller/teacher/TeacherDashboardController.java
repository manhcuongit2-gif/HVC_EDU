package com.educenter.controller.teacher;

import com.educenter.entity.ClassEntity;
import com.educenter.entity.Schedule;
import com.educenter.repository.ClassRepository;
import com.educenter.repository.TeacherRepository;
import com.educenter.security.CustomUserDetails;
import com.educenter.service.ClassService;
import com.educenter.service.ScheduleService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/teacher")
public class TeacherDashboardController {

    private final ClassRepository classRepository;
    private final TeacherRepository teacherRepository;
    private final ClassService classService;
    private final ScheduleService scheduleService;

    public TeacherDashboardController(ClassRepository classRepository,
                                    TeacherRepository teacherRepository,
                                    ClassService classService,
                                    ScheduleService scheduleService) {
        this.classRepository = classRepository;
        this.teacherRepository = teacherRepository;
        this.classService = classService;
        this.scheduleService = scheduleService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        List<ClassEntity> myClasses = classRepository.findByTeacherUserId(userDetails.getId());
        List<Schedule> mySchedules = scheduleService.getSchedulesByTeacherUserId(userDetails.getId());

        long totalStudents = 0;
        Map<Long, Long> studentCounts = new HashMap<>();
        for (ClassEntity c : myClasses) {
            long count = classService.getStudentCountInClass(c.getId());
            studentCounts.put(c.getId(), count);
            totalStudents += count;
        }

        model.addAttribute("currentUser", userDetails);
        model.addAttribute("myClasses", myClasses);
        model.addAttribute("mySchedules", mySchedules);
        model.addAttribute("studentCounts", studentCounts);
        model.addAttribute("classCount", myClasses.size());
        model.addAttribute("totalStudents", totalStudents);
        model.addAttribute("scheduleCount", mySchedules.size());
        model.addAttribute("canManageStudents", teacherRepository.findByUserId(userDetails.getId())
                .map(teacher -> teacher.isCanManageStudents()).orElse(false));
        return "teacher/dashboard";
    }
}
