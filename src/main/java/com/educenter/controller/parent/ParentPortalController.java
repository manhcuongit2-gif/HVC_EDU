package com.educenter.controller.parent;

import com.educenter.entity.*;
import com.educenter.repository.*;
import com.educenter.security.CustomUserDetails;
import com.educenter.service.ClassService;
import com.educenter.service.RegistrationService;
import com.educenter.service.TuitionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.*;

@Controller
@RequestMapping("/parent")
public class ParentPortalController {

    private final ParentRepository parentRepository;
    private final StudentRepository studentRepository;
    private final ClassRepository classRepository;
    private final ClassStudentRepository classStudentRepository;
    private final RegistrationService registrationService;
    private final TuitionService tuitionService;
    private final GradeRepository gradeRepository;
    private final AttendanceRepository attendanceRepository;
    private final ScheduleRepository scheduleRepository;

    public ParentPortalController(ParentRepository parentRepository,
                                  StudentRepository studentRepository,
                                  ClassRepository classRepository,
                                  ClassStudentRepository classStudentRepository,
                                  RegistrationService registrationService,
                                  TuitionService tuitionService,
                                  GradeRepository gradeRepository,
                                  AttendanceRepository attendanceRepository,
                                  ScheduleRepository scheduleRepository) {
        this.parentRepository = parentRepository;
        this.studentRepository = studentRepository;
        this.classRepository = classRepository;
        this.classStudentRepository = classStudentRepository;
        this.registrationService = registrationService;
        this.tuitionService = tuitionService;
        this.gradeRepository = gradeRepository;
        this.attendanceRepository = attendanceRepository;
        this.scheduleRepository = scheduleRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        Parent parent = parentRepository.findByUserId(userDetails.getId()).orElse(null);
        List<Student> myChildren = (parent != null) ? studentRepository.findByParentId(parent.getId()) : Collections.emptyList();
        List<Tuition> myTuitions = tuitionService.getTuitionByParentUserId(userDetails.getId());

        long unpaidCount = myTuitions.stream().filter(t -> "UNPAID".equals(t.getStatus())).count();
        List<Registration> myRegistrations = registrationService.getRegistrationsByParentUserId(userDetails.getId());

        model.addAttribute("currentUser", userDetails);
        model.addAttribute("parent", parent);
        model.addAttribute("myChildren", myChildren);
        model.addAttribute("myTuitions", myTuitions);
        model.addAttribute("unpaidCount", unpaidCount);
        model.addAttribute("myRegistrations", myRegistrations);
        return "parent/dashboard";
    }

    @GetMapping("/children")
    public String myChildren(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        Parent parent = parentRepository.findByUserId(userDetails.getId()).orElse(null);
        List<Student> children = (parent != null) ? studentRepository.findByParentId(parent.getId()) : Collections.emptyList();

        Map<Long, List<ClassEntity>> studentClasses = new HashMap<>();
        for (Student st : children) {
            List<ClassStudent> enrollments = classStudentRepository.findByStudentId(st.getId());
            List<ClassEntity> classes = new ArrayList<>();
            for (ClassStudent cs : enrollments) {
                if ("ACTIVE".equals(cs.getStatus())) {
                    classes.add(cs.getClassEntity());
                }
            }
            studentClasses.put(st.getId(), classes);
        }

        model.addAttribute("currentUser", userDetails);
        model.addAttribute("children", children);
        model.addAttribute("studentClasses", studentClasses);
        return "parent/children";
    }

    @PostMapping("/children/add")
    public String addChild(@AuthenticationPrincipal CustomUserDetails userDetails,
                           @RequestParam String fullName,
                           @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dateOfBirth,
                           @RequestParam(required = false) String gender,
                           @RequestParam(required = false) String phone,
                           @RequestParam(required = false) String school,
                           @RequestParam(required = false) String gradeLevel,
                           RedirectAttributes redirectAttributes) {
        try {
            Parent parent = parentRepository.findByUserId(userDetails.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin phụ huynh"));

            Student s = new Student();
            s.setFullName(fullName);
            s.setDateOfBirth(dateOfBirth);
            s.setGender(gender != null ? gender : "Nam");
            s.setPhone(phone);
            s.setSchool(school);
            s.setGradeLevel(gradeLevel);
            s.setParent(parent);
            s.setActive(true);
            studentRepository.save(s);

            redirectAttributes.addFlashAttribute("successMessage", "Thêm hồ sơ con thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/parent/children";
    }

    @GetMapping("/register-class")
    public String showRegisterClassForm(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        Parent parent = parentRepository.findByUserId(userDetails.getId()).orElse(null);
        List<Student> myChildren = (parent != null) ? studentRepository.findByParentId(parent.getId()) : Collections.emptyList();
        List<ClassEntity> activeClasses = classRepository.findByStatus("ACTIVE");

        model.addAttribute("currentUser", userDetails);
        model.addAttribute("myChildren", myChildren);
        model.addAttribute("classes", activeClasses);
        return "parent/register-class";
    }

    @PostMapping("/register-class")
    public String registerClass(@AuthenticationPrincipal CustomUserDetails userDetails,
                                @RequestParam Long studentId,
                                @RequestParam Long classId,
                                @RequestParam(required = false) String note,
                                RedirectAttributes redirectAttributes) {
        try {
            registrationService.registerClass(userDetails.getId(), studentId, classId, note);
            redirectAttributes.addFlashAttribute("successMessage", "Gửi đơn đăng ký học thành công! Vui lòng chờ nhân viên trung tâm phê duyệt.");
            return "redirect:/parent/registrations";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/parent/register-class";
        }
    }

    @GetMapping("/registrations")
    public String myRegistrations(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        List<Registration> registrations = registrationService.getRegistrationsByParentUserId(userDetails.getId());
        model.addAttribute("currentUser", userDetails);
        model.addAttribute("registrations", registrations);
        return "parent/registrations";
    }

    @GetMapping("/grades")
    public String myChildrenGrades(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        Parent parent = parentRepository.findByUserId(userDetails.getId()).orElse(null);
        List<Student> children = (parent != null) ? studentRepository.findByParentId(parent.getId()) : Collections.emptyList();

        Map<Long, List<Grade>> studentGrades = new HashMap<>();
        Map<Long, List<Attendance>> studentAttendances = new HashMap<>();

        for (Student st : children) {
            studentGrades.put(st.getId(), gradeRepository.findByStudentId(st.getId()));
            studentAttendances.put(st.getId(), attendanceRepository.findByStudentIdOrderByAttendanceDateDesc(st.getId()));
        }

        model.addAttribute("currentUser", userDetails);
        model.addAttribute("children", children);
        model.addAttribute("studentGrades", studentGrades);
        model.addAttribute("studentAttendances", studentAttendances);
        return "parent/grades";
    }

    @GetMapping("/tuition")
    public String myTuition(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        List<Tuition> tuitions = tuitionService.getTuitionByParentUserId(userDetails.getId());
        long unpaidCount = tuitions.stream().filter(t -> "UNPAID".equals(t.getStatus())).count();

        model.addAttribute("currentUser", userDetails);
        model.addAttribute("tuitions", tuitions);
        model.addAttribute("unpaidCount", unpaidCount);
        return "parent/tuition";
    }
}
