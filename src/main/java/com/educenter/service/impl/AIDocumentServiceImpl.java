package com.educenter.service.impl;

import com.educenter.config.AIProperties;
import com.educenter.service.AIDocumentService;
import com.educenter.service.AIDocumentService.DocumentPart;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class AIDocumentServiceImpl implements AIDocumentService {
    private final AIProperties properties;

    public AIDocumentServiceImpl(AIProperties properties) { this.properties = properties; }

    @Override
    public String extractText(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Vui lòng chọn tệp tài liệu.");
        try {
            return extractParts(file.getBytes(), file.getOriginalFilename()).stream()
                    .map(DocumentPart::text).filter(text -> text != null && !text.isBlank())
                    .collect(Collectors.joining("\n\n"));
        } catch (IOException e) {
            throw new IllegalArgumentException("Không thể đọc tệp. Hãy kiểm tra tệp có hợp lệ và không bị khóa.");
        }
    }

    @Override
    public List<DocumentPart> extractParts(byte[] bytes, String filename) {
        if (bytes == null || bytes.length == 0) throw new IllegalArgumentException("Tệp tài liệu đang trống.");
        if (bytes.length > properties.getGroq().getMaxUploadBytes())
            throw new IllegalArgumentException("Tệp vượt quá giới hạn AI hiện tại (10 MB).");
        String name = filename == null ? "" : filename;
        String extension = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        try {
            List<DocumentPart> parts = switch (extension) {
                case "txt" -> List.of(new DocumentPart(new String(bytes, StandardCharsets.UTF_8), null, null));
                case "pdf" -> extractPdfParts(bytes);
                case "docx" -> List.of(new DocumentPart(extractDocx(bytes), null, null));
                case "doc" -> List.of(new DocumentPart(extractDoc(bytes), null, null));
                case "xlsx", "xls" -> extractWorkbookParts(bytes);
                default -> throw new IllegalArgumentException("Chỉ hỗ trợ PDF, Word, Excel và TXT.");
            };
            return limitParts(parts);
        } catch (IOException e) {
            throw new IllegalArgumentException("Không thể đọc tệp. Hãy kiểm tra tệp có hợp lệ và không bị khóa.");
        }
    }

    private List<DocumentPart> limitParts(List<DocumentPart> parts) {
        int remaining = properties.getGroq().getMaxExtractedCharacters();
        List<DocumentPart> result = new ArrayList<>();
        for (DocumentPart part : parts) {
            if (remaining <= 0) break;
            String normalized = part.text() == null ? "" : part.text().replace('\u0000', ' ').trim();
            if (normalized.isBlank()) continue;
            boolean truncated = normalized.length() > remaining;
            result.add(new DocumentPart(truncated ? normalized.substring(0, remaining) : normalized,
                    part.pageNumber(), part.sectionName()));
            remaining -= Math.min(remaining, normalized.length());
            if (truncated) break;
        }
        if (result.isEmpty()) throw new IllegalArgumentException("Tệp không có nội dung văn bản có thể trích xuất.");
        return List.copyOf(result);
    }

    private List<DocumentPart> extractPdfParts(byte[] bytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            if (document.isEncrypted()) throw new IllegalArgumentException("Không hỗ trợ PDF được mã hóa.");
            PDFTextStripper stripper = new PDFTextStripper();
            List<DocumentPart> parts = new ArrayList<>();
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                parts.add(new DocumentPart(stripper.getText(document), page, null));
            }
            return parts;
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

    private List<DocumentPart> extractWorkbookParts(byte[] bytes) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            DataFormatter formatter = new DataFormatter(Locale.forLanguageTag("vi-VN"));
            List<DocumentPart> parts = new ArrayList<>();
            for (var sheet : workbook) {
                StringBuilder text = new StringBuilder();
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
                parts.add(new DocumentPart(text.toString(), null, sheet.getSheetName()));
            }
            return parts;
        }
    }
}
