package com.educenter.dto;

import com.educenter.entity.*;

import java.util.List;

public record StudentPortalData(
        Student student,
        List<ClassStudent> activeEnrollments,
        List<ClassSession> upcomingSessions,
        List<StudentHomeworkRow> homeworkRows,
        List<Grade> grades,
        List<StudentAttendanceRow> attendance,
        List<SessionStudentReport> sessionReports,
        List<MonthlyStudentReport> monthlyReports,
        long presentCount,
        long absentCount,
        long excusedCount,
        long pendingHomeworkCount,
        long overdueHomeworkCount,
        long submittedHomeworkCount) {
}
