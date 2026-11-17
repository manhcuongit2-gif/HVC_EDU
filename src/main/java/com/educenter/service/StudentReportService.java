package com.educenter.service;

import com.educenter.entity.ClassSession;
import com.educenter.entity.MonthlyStudentReport;
import com.educenter.entity.SessionStudentReport;

import java.time.YearMonth;
import java.util.List;

public interface StudentReportService {
    ClassSession getTeacherSession(Long sessionId, Long teacherUserId);
    List<SessionStudentReport> getTeacherSessionReports(Long sessionId, Long teacherUserId);
    List<SessionStudentReport> getAdminSessionReports(Long sessionId);
    void saveSessionReports(Long sessionId, Long teacherUserId, List<Long> studentIds,
                            List<String> attendanceStatuses, List<String> homeworkScores,
                            List<String> comments, List<String> goodEvaluationCounts, List<String> notes);
    void publishSession(Long sessionId, Long teacherUserId);
    List<MonthlyStudentReport> getMonthlyReports(Long classId, YearMonth month, Long teacherUserId);
    boolean publishMonthlyReport(Long classId, Long studentId, YearMonth month, String comment, Long teacherUserId);
    List<SessionStudentReport> getPublishedSessionReportsForParent(Long parentUserId);
    List<MonthlyStudentReport> getPublishedMonthlyReportsForParent(Long parentUserId);
}
