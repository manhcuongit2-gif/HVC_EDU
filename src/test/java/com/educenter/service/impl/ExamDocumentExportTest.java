package com.educenter.service.impl;

import com.educenter.dto.ExamExportDocument;
import com.educenter.dto.ExamExportQuestion;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ExamDocumentExportTest {
    private final AIExportServiceImpl exporter = new AIExportServiceImpl();

    @Test
    void exportsFormulaAsDocumentAndPdfWithoutChangingLegacyExport() {
        ExamExportDocument exam = new ExamExportDocument("Bài kiểm tra", "Toán · Lớp 6", List.of(
                new ExamExportQuestion(1, "MULTIPLE_CHOICE", "Tính $\\frac{1}{2}$", new BigDecimal("10.00"),
                        List.of(new ExamExportQuestion.ExamExportOption("A", "$0.5$")), "A: $0.5$", "Quy đồng mẫu số.")));
        byte[] word = exporter.exportExamWord(exam);
        byte[] pdf = exporter.exportExamPdf(exam);
        assertEquals('P', word[0]); assertEquals('K', word[1]);
        assertArrayEquals(new byte[]{'%', 'P', 'D', 'F'}, java.util.Arrays.copyOf(pdf, 4));
        assertTrue(exporter.exportWord("AI", "$x+1$").length > 0);
    }
}
