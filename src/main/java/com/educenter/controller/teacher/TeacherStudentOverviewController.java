package com.educenter.controller.teacher;

import com.educenter.entity.ClassEntity;
import com.educenter.entity.Teacher;
import com.educenter.repository.ClassRepository;
import com.educenter.repository.ClassStudentRepository;
import com.educenter.repository.TeacherRepository;
import com.educenter.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/teacher/students")
public class TeacherStudentOverviewController {

    private final ClassRepository classRepository;
    private final ClassStudentRepository classStudentRepository;
    private final TeacherRepository teacherRepository;

    public TeacherStudentOverviewController(ClassRepository classRepository,
                                            ClassStudentRepository classStudentRepository,
                                            TeacherRepository teacherRepository) {
        this.classRepository = classRepository;
        this.classStudentRepository = classStudentRepository;
        this.teacherRepository = teacherRepository;
    }

    @GetMapping
    public String index(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        Teacher teacher = teacherRepository.findByUserId(user.getId()).orElse(null);
        List<ClassEntity> classes = classRepository.findByTeacherUserId(user.getId());
        Map<Long, Long> studentCounts = new HashMap<>();
        classes.forEach(c -> studentCounts.put(c.getId(),
                classStudentRepository.countByClassEntityIdAndStatus(c.getId(), "ACTIVE")));
        model.addAttribute("currentUser", user);
        model.addAttribute("classes", classes);
        model.addAttribute("studentCounts", studentCounts);
        model.addAttribute("canManageStudents", teacher != null && teacher.isCanManageStudents());
        return "teacher/students";
    }
}
