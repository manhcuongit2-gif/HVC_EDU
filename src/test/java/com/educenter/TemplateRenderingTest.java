package com.educenter;

import com.educenter.dto.CalendarDay;
import com.educenter.entity.ClassEntity;
import com.educenter.entity.ClassSession;
import com.educenter.entity.Teacher;
import com.educenter.entity.User;
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
import java.time.YearMonth;
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

    private WebContext webContext() {
        JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(new MockServletContext());
        return new WebContext(application.buildExchange(new MockHttpServletRequest(), new MockHttpServletResponse()), Locale.forLanguageTag("vi"));
    }
}
