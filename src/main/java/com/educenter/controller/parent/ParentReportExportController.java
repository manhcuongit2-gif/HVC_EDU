package com.educenter.controller.parent;

import com.educenter.entity.MonthlyStudentReport;
import com.educenter.entity.SessionStudentReport;
import com.educenter.security.CustomUserDetails;
import com.educenter.service.ReportFileService;
import com.educenter.service.StudentReportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
@RequestMapping("/parent/results")
public class ParentReportExportController {
    private final StudentReportService reportService;
    private final ReportFileService fileService;

    public ParentReportExportController(StudentReportService reportService, ReportFileService fileService) {
        this.reportService = reportService;
        this.fileService = fileService;
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam(defaultValue = "session") String view,
                                         @RequestParam(defaultValue = "xlsx") String format,
                                         @AuthenticationPrincipal CustomUserDetails user) {
        boolean pdf = "pdf".equalsIgnoreCase(format);
        if (!pdf && !"xlsx".equalsIgnoreCase(format)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Định dạng chỉ hỗ trợ xlsx hoặc pdf.");
        byte[] bytes;
        String baseName;
        if ("session".equalsIgnoreCase(view)) {
            List<SessionStudentReport> reports = reportService.getPublishedSessionReportsForParent(user.getId());
            bytes = pdf ? fileService.exportSessionPdf("Kết quả điểm danh", reports)
                    : fileService.exportSessionExcel("Kết quả điểm danh", reports);
            baseName = "ket-qua-diem-danh";
        } else if ("monthly".equalsIgnoreCase(view)) {
            List<MonthlyStudentReport> reports = reportService.getPublishedMonthlyReportsForParent(user.getId());
            bytes = pdf ? fileService.exportMonthlyPdf("Tổng kết tháng", reports)
                    : fileService.exportMonthlyExcel("Tổng kết tháng", reports);
            baseName = "tong-ket-thang";
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loại báo cáo không hợp lệ.");
        }
        return attachment(bytes, baseName + (pdf ? ".pdf" : ".xlsx"), pdf ? "application/pdf"
                : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    private ResponseEntity<byte[]> attachment(byte[] bytes, String filename, String contentType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(bytes);
    }
}
