package com.educenter.service.impl;

import com.educenter.entity.ClassEntity;
import com.educenter.entity.ClassSession;
import com.educenter.entity.MonthlyStudentReport;
import com.educenter.entity.SessionStudentReport;
import com.educenter.entity.Student;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportFileServiceImplTest {
    private final ReportFileServiceImpl service = new ReportFileServiceImpl();

    @Test
    void exportsSessionReportAsVietnameseTable() throws Exception {
        byte[] bytes = service.exportSessionPdf("Báo cáo buổi học", List.of(sessionReport(101L, "Nguyễn Minh Khôi",
                "Nhận xét: con đã tiến bộ rõ rệt.")));

        try (PDDocument document = Loader.loadPDF(bytes)) {
            assertTrue(document.getNumberOfPages() >= 1);
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("Ngày"));
            assertTrue(text.contains("Học sinh"));
            assertTrue(text.contains("Điểm danh"));
            assertTrue(text.contains("Điểm Cộng"));
            assertTrue(text.contains("Nguyễn Minh Khôi"));
            assertTrue(text.contains("Nhận xét: con đã tiến bộ rõ rệt."));
        }
    }

    @Test
    void exportsEmptySessionAndMonthlyReportsAsValidPdfs() throws Exception {
        byte[] sessionBytes = service.exportSessionPdf("Báo cáo buổi học", List.of());
        byte[] monthlyBytes = service.exportMonthlyPdf("Tổng kết tháng", List.of());

        try (PDDocument session = Loader.loadPDF(sessionBytes); PDDocument monthly = Loader.loadPDF(monthlyBytes)) {
            assertEquals(1, session.getNumberOfPages());
            assertEquals(1, monthly.getNumberOfPages());
            assertTrue(new PDFTextStripper().getText(session).contains("Chưa có dữ liệu báo cáo."));
        }
    }

    @Test
    void exportsPopulatedMonthlyNarrativeAsValidVietnamesePdf() throws Exception {
        ClassEntity classEntity = new ClassEntity();
        classEntity.setName("Toán lớp 7");
        Student student = new Student();
        student.setFullName("Nguyễn Minh Khôi");
        MonthlyStudentReport report = new MonthlyStudentReport();
        report.setClassEntity(classEntity);
        report.setStudent(student);
        report.setReportMonth(LocalDate.of(2026, 11, 1));
        report.setPresentCount(7);
        report.setAbsentCount(1);
        report.setExcusedCount(0);
        report.setHomeworkAverage(new BigDecimal("8.75"));
        report.setGoodEvaluationCount(3);
        report.setTeacherComment("Tiến bộ tốt trong tháng.");

        byte[] bytes = service.exportMonthlyPdf("Tổng kết tháng", List.of(report));

        try (PDDocument document = Loader.loadPDF(bytes)) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("Tổng kết tháng"));
            assertTrue(text.contains("Nguyễn Minh Khôi"));
            assertTrue(text.contains("Tiến bộ tốt trong tháng."));
        }
    }

    @Test
    void wrapsLongCommentsAndRepeatsHeadersAcrossPages() throws Exception {
        String longComment = "Cần củng cố phép tính phân số và luyện tập thêm mỗi ngày. ".repeat(18);
        List<SessionStudentReport> reports = new ArrayList<>();
        for (long i = 1; i <= 12; i++) reports.add(sessionReport(i, "Học sinh " + i, longComment));

        byte[] bytes = service.exportSessionPdf("Báo cáo lớp 7", reports);

        try (PDDocument document = Loader.loadPDF(bytes)) {
            assertTrue(document.getNumberOfPages() > 1);
            PDFTextStripper stripper = new PDFTextStripper();
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                assertTrue(stripper.getText(document).contains("Điểm danh"));
            }
            assertTrue(new PDFTextStripper().getText(document).contains("mỗi ngày."));
        }
    }

    private SessionStudentReport sessionReport(Long id, String studentName, String comment) {
        ClassEntity classEntity = new ClassEntity();
        classEntity.setName("Toán lớp 7");

        ClassSession session = new ClassSession();
        session.setClassEntity(classEntity);
        session.setSessionDate(LocalDate.of(2026, 11, 21));

        Student student = new Student();
        student.setId(id);
        student.setFullName(studentName);

        SessionStudentReport report = new SessionStudentReport();
        report.setClassSession(session);
        report.setStudent(student);
        report.setAttendanceStatus("PRESENT");
        report.setHomeworkScore(new BigDecimal("9.50"));
        report.setComment(comment);
        report.setGoodEvaluationCount(2);
        report.setNote("Tiếp tục duy trì.");
        return report;
    }
}
