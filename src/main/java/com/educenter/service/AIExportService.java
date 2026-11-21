package com.educenter.service;

import com.educenter.dto.ExamExportDocument;

public interface AIExportService {
    byte[] exportWord(String title, String content);
    byte[] exportPdf(String title, String content);
    byte[] exportExamWord(ExamExportDocument document);
    byte[] exportExamPdf(ExamExportDocument document);
}
