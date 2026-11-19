package com.educenter;

import com.educenter.dto.CalendarDay;
import com.educenter.dto.TeacherEarningRow;
import com.educenter.dto.TeacherEarningsPage;
import com.educenter.entity.ClassEntity;
import com.educenter.entity.ClassSession;
import com.educenter.entity.Role;
import com.educenter.entity.Teacher;
import com.educenter.entity.TeacherAdminMessage;
import com.educenter.entity.User;
import com.educenter.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.math.BigDecimal;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TemplateRenderingTest {

    private TemplateEngine templateEngine;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCacheable(false);
        templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(resolver);
    }

    @Test
    void rendersAdminScheduleCalendarWithSessions() {
        ClassEntity classEntity = new ClassEntity();
        classEntity.setName("Toán lớp 7");
        ClassSession session = new ClassSession();
        session.setId(17L);
        session.setClassEntity(classEntity);
        session.setSessionDate(LocalDate.of(2026, 11, 19));
        session.setStartTime(LocalTime.of(17, 30));
        session.setEndTime(LocalTime.of(19, 0));
        session.setStatus("SCHEDULED");
        WebContext context = webContext();
        context.setVariable("month", YearMonth.of(2026, 11));
        context.setVariable("previousMonth", YearMonth.of(2026, 10));
        context.setVariable("nextMonth", YearMonth.of(2026, 12));
        context.setVariable("selectedClassId", null);
        context.setVariable("classes", List.of());
        context.setVariable("classesMissingDates", List.of());
        context.setVariable("calendarDays", List.of(new CalendarDay(LocalDate.of(2026, 11, 19), List.of(session), true)));

        String html = templateEngine.process("admin/schedules/calendar", context);

        assertTrue(html.contains("Toán lớp 7"));
        assertTrue(html.contains("17:30"));
        assertTrue(html.contains("Thứ Năm"));
    }

    @Test
    void rendersTeacherSessionReportWhenNoStudentsAreEnrolled() {
        ClassEntity classEntity = new ClassEntity();
        classEntity.setName("Toán lớp 7");
        ClassSession session = new ClassSession();
        session.setId(31L);
        session.setClassEntity(classEntity);
        session.setSessionDate(LocalDate.of(2026, 11, 19));
        session.setStartTime(LocalTime.of(17, 30));
        session.setEndTime(LocalTime.of(19, 0));
        WebContext context = webContext();
        context.setVariable("classSession", session);
        context.setVariable("reports", List.of());

        String html = templateEngine.process("teacher/session-report", context);

        assertTrue(html.contains("Báo cáo từng buổi"));
        assertTrue(html.contains("Lớp chưa có học sinh"));
    }

    @Test
    void rendersAdminScheduleSessionDetail() {
        User teacherUser = new User();
        teacherUser.setFullName("Giáo viên phụ trách");
        Teacher teacher = new Teacher();
        teacher.setUser(teacherUser);
        ClassEntity classEntity = new ClassEntity();
        classEntity.setName("Toán lớp 7");
        classEntity.setTeacher(teacher);
        ClassSession session = new ClassSession();
        session.setId(42L);
        session.setClassEntity(classEntity);
        session.setSessionDate(LocalDate.of(2026, 11, 19));
        session.setStartTime(LocalTime.of(17, 30));
        session.setEndTime(LocalTime.of(19, 0));
        session.setStatus("SCHEDULED");
        WebContext context = webContext();
        context.setVariable("classSession", session);
        context.setVariable("reports", List.of());
        context.setVariable("rooms", List.of());

        String html = templateEngine.process("admin/schedules/session", context);

        assertTrue(html.contains("Toán lớp 7"));
        assertTrue(html.contains("Giáo viên phụ trách"));
    }

    @Test
    void rendersTeacherAdminConversationThread() {
        User teacherUser = new User();
        teacherUser.setId(12L);
        teacherUser.setFullName("Teacher");
        teacherUser.setRole(new Role(2, "ROLE_TEACHER", "Teacher"));
        TeacherAdminMessage message = new TeacherAdminMessage();
        message.setSender(teacherUser);
        message.setContent("Hello admin");
        message.setCreatedAt(LocalDateTime.of(2026, 11, 19, 10, 0));

        WebContext context = webContext();
        context.setVariable("currentUser", new CustomUserDetails(teacherUser));
        context.setVariable("title", "Admin conversation");
        context.setVariable("messages", List.of(message));
        context.setVariable("postUrl", "/teacher/admin-messages");
        context.setVariable("backUrl", "/teacher/dashboard");
        context.setVariable("backLabel", "Dashboard");
        context.setVariable("canSend", true);

        String html = templateEngine.process("messages/thread", context);

        assertTrue(html.contains("Admin conversation"));
        assertTrue(html.contains("Hello admin"));
        assertTrue(html.contains("/teacher/admin-messages"));
    }

    @Test
    void rendersAdminTeacherConversationList() {
        User adminUser = new User();
        adminUser.setId(1L);
        adminUser.setFullName("Admin");
        adminUser.setRole(new Role(1, "ROLE_ADMIN", "Admin"));
        User teacherUser = new User();
        teacherUser.setId(12L);
        teacherUser.setFullName("Math teacher");
        teacherUser.setRole(new Role(2, "ROLE_TEACHER", "Teacher"));
        Teacher teacher = new Teacher();
        teacher.setUser(teacherUser);
        teacher.setSpecialization("Mathematics");

        WebContext context = webContext();
        context.setVariable("currentUser", new CustomUserDetails(adminUser));
        context.setVariable("teachers", List.of(teacher));

        String html = templateEngine.process("admin/messages/list", context);

        assertTrue(html.contains("Math teacher"));
        assertTrue(html.contains("/admin/messages/12"));
    }

    @Test
    void rendersTeacherEarningsWithVietnameseWeekday() {
        User teacherUser = new User();
        teacherUser.setId(12L);
        teacherUser.setFullName("Giáo viên");
        teacherUser.setRole(new Role(2, "ROLE_TEACHER", "Giáo viên"));
        ClassEntity classEntity = new ClassEntity();
        classEntity.setName("Toán lớp 7");
        ClassSession session = new ClassSession();
        session.setClassEntity(classEntity);
        session.setSessionDate(LocalDate.of(2026, 11, 19));
        session.setStartTime(LocalTime.of(17, 30));
        session.setEndTime(LocalTime.of(19, 0));
        TeacherEarningsPage page = new TeacherEarningsPage(
                List.of(new TeacherEarningRow(session, new BigDecimal("150000"), new BigDecimal("150000"))),
                new BigDecimal("150000"), new BigDecimal("150000"), YearMonth.of(2026, 11));

        WebContext context = webContext();
        context.setVariable("currentUser", new CustomUserDetails(teacherUser));
        context.setVariable("earnings", page);
        context.setVariable("previousMonth", YearMonth.of(2026, 10));
        context.setVariable("nextMonth", YearMonth.of(2026, 12));

        String html = templateEngine.process("teacher/earnings", context);

        assertTrue(html.contains("Thứ Năm"));
        assertTrue(html.contains("Toán lớp 7"));
        assertTrue(html.contains("150,000"));
    }

    @Test
    void rendersTeacherClassRosterPage() {
        User teacherUser = new User();
        teacherUser.setId(12L);
        teacherUser.setFullName("Giáo viên");
        teacherUser.setRole(new Role(2, "ROLE_TEACHER", "Giáo viên"));
        ClassEntity classEntity = new ClassEntity();
        classEntity.setName("Toán lớp 7");

        WebContext context = webContext();
        context.setVariable("currentUser", new CustomUserDetails(teacherUser));
        context.setVariable("classEntity", classEntity);
        context.setVariable("enrolledStudents", List.of());
        context.setVariable("availableStudents", List.of());
        context.setVariable("parents", List.of());
        context.setVariable("studentForm", new com.educenter.dto.TeacherStudentForm());

        String html = templateEngine.process("teacher/class-students", context);

        assertTrue(html.contains("Quản lý học sinh"));
        assertTrue(html.contains("Tạo hồ sơ học sinh mới"));
        assertTrue(html.contains("Toán lớp 7"));
    }

    @Test
    void parentDashboardPrioritizesRecentLearningReport() throws IOException {
        try (var input = getClass().getClassLoader().getResourceAsStream("templates/parent/dashboard.html")) {
            assertTrue(input != null);
            String template = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(template.indexOf("Báo cáo gần nhất và lịch học sắp tới") < template.indexOf("Truy cập nhanh"));
        }
    }

    @Test
    void loginPageUsesAcademicVisuals() {
        String html = templateEngine.process("auth/login", webContext());

        assertTrue(html.contains("school-mark"));
        assertTrue(html.contains("bi-book-half"));
        assertTrue(html.contains("TRUNG TÂM HỌC TẬP"));
    }

    private WebContext webContext() {
        JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(new MockServletContext());
        return new WebContext(application.buildExchange(new MockHttpServletRequest(), new MockHttpServletResponse()), Locale.forLanguageTag("vi"));
    }
}
