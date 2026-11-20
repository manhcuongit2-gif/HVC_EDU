package com.educenter.service.impl;

import com.educenter.config.AIProperties;
import com.educenter.service.AIDocumentService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hssf.extractor.ExcelExtractor;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class AIDocumentServiceImpl implements AIDocumentService {
    private final AIProperties properties;

    public AIDocumentServiceImpl(AIProperties properties) { this.properties = properties; }

    @Override
    public String extractText(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Vui lòng chọn tệp tài liệu.");
        if (file.getSize() > properties.getGroq().getMaxUploadBytes())
            throw new IllegalArgumentException("Tệp vượt quá kích thước cho phép (10 MB).");
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String extension = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        try {
            String extracted = switch (extension) {
                case "txt" -> new String(file.getBytes(), StandardCharsets.UTF_8);
                case "pdf" -> extractPdf(file.getBytes());
                case "docx" -> extractDocx(file.getBytes());
                case "doc" -> extractDoc(file.getBytes());
                case "xlsx", "xls" -> extractWorkbook(file.getBytes());
                default -> throw new IllegalArgumentException("Chỉ hỗ trợ PDF, Word, Excel và TXT.");
            };
            String normalized = extracted.replace('\u0000', ' ').trim();
            if (normalized.isBlank()) throw new IllegalArgumentException("Tệp không có nội dung văn bản có thể trích xuất.");
            int max = properties.getGroq().getMaxExtractedCharacters();
            return normalized.length() <= max ? normalized : normalized.substring(0, max) + "\n[Đã cắt bớt nội dung theo giới hạn an toàn]";
        } catch (IOException e) {
            throw new IllegalArgumentException("Không thể đọc tệp. Hãy kiểm tra tệp có hợp lệ và không bị khóa.");
        }
    }

    private String extractPdf(byte[] bytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            if (document.isEncrypted()) throw new IllegalArgumentException("Không hỗ trợ PDF được mã hóa.");
            return new PDFTextStripper().getText(document);
        }
    }

    private String extractDocx(byte[] bytes) throws IOException {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes));
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String extractDoc(byte[] bytes) throws IOException {
        try (HWPFDocument document = new HWPFDocument(new ByteArrayInputStream(bytes));
             WordExtractor extractor = new WordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String extractWorkbook(byte[] bytes) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            DataFormatter formatter = new DataFormatter(Locale.forLanguageTag("vi-VN"));
            StringBuilder text = new StringBuilder();
            for (var sheet : workbook) {
                text.append("\n## ").append(sheet.getSheetName()).append('\n');
                for (Row row : sheet) {
                    boolean first = true;
                    for (var cell : row) {
                        if (!first) text.append(" | ");
                        first = false;
                        text.append(formatter.formatCellValue(cell));
                    }
                    text.append('\n');
                    if (text.length() > properties.getGroq().getMaxExtractedCharacters()) break;
                }
                if (text.length() > properties.getGroq().getMaxExtractedCharacters()) break;
            }
            return text.toString();
        }
    }
}
