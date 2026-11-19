package com.educenter.service;

import com.educenter.dto.SessionReportImportPreview;
import com.educenter.entity.ClassSession;
import com.educenter.entity.MonthlyStudentReport;
import com.educenter.entity.SessionStudentReport;

import java.io.InputStream;
import java.util.List;

public interface ReportFileService {
    byte[] createSessionTemplate(ClassSession session, List<SessionStudentReport> reports);
    SessionReportImportPreview parseSessionWorkbook(InputStream inputStream);
    byte[] exportSessionExcel(String title, List<SessionStudentReport> reports);
    byte[] exportMonthlyExcel(String title, List<MonthlyStudentReport> reports);
    byte[] exportSessionPdf(String title, List<SessionStudentReport> reports);
    byte[] exportMonthlyPdf(String title, List<MonthlyStudentReport> reports);
}
