package com.educenter.service.impl;

import com.educenter.service.AIExportService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class AIExportServiceImpl implements AIExportService {
    @Override
    public byte[] exportWord(String title, String content) {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            XWPFParagraph heading = document.createParagraph();
            heading.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = heading.createRun();
            titleRun.setText(safe(title)); titleRun.setBold(true); titleRun.setFontSize(16);
            for (String line : safe(content).split("\\R", -1)) {
                XWPFParagraph paragraph = document.createParagraph();
                XWPFRun run = paragraph.createRun();
                run.setFontFamily("Arial"); run.setFontSize(11); run.setText(line);
            }
            document.write(output);
            return output.toByteArray();
        } catch (IOException e) { throw new IllegalStateException("Không thể tạo tệp Word."); }
    }

    @Override
    public byte[] exportPdf(String title, String content) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream();
             InputStream fontStream = getClass().getResourceAsStream("/fonts/NotoSans-Vietnamese.ttf")) {
            if (fontStream == null) throw new IllegalStateException("Thiếu phông chữ tiếng Việt.");
            PDType0Font font = PDType0Font.load(document, fontStream);
            List<String> lines = wrap(safe(content), font, 10, 520);
            List<List<String>> pages = new ArrayList<>();
            for (int i = 0; i < lines.size(); i += 42) pages.add(lines.subList(i, Math.min(i + 42, lines.size())));
            if (pages.isEmpty()) pages.add(List.of(""));
            for (int p = 0; p < pages.size(); p++) {
                PDPage page = new PDPage(PDRectangle.LETTER);
                document.addPage(page);
                try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                    stream.beginText(); stream.setFont(font, 15); stream.newLineAtOffset(48, 748); stream.showText(safe(title)); stream.endText();
                    float y = 720;
                    for (String line : pages.get(p)) {
                        stream.beginText(); stream.setFont(font, 10); stream.newLineAtOffset(48, y); stream.showText(line); stream.endText();
                        y -= 14;
                    }
                    stream.beginText(); stream.setFont(font, 8); stream.newLineAtOffset(500, 25); stream.showText((p + 1) + " / " + pages.size()); stream.endText();
                }
            }
            document.save(output);
            return output.toByteArray();
        } catch (IOException e) { throw new IllegalStateException("Không thể tạo tệp PDF."); }
    }

    private List<String> wrap(String content, PDType0Font font, float fontSize, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        for (String paragraph : content.split("\\R", -1)) {
            StringBuilder line = new StringBuilder();
            for (String word : paragraph.split("\\s+")) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (font.getStringWidth(candidate) / 1000f * fontSize > maxWidth && !line.isEmpty()) {
                    lines.add(line.toString()); line.setLength(0); line.append(word);
                } else { if (!line.isEmpty()) line.append(' '); line.append(word); }
            }
            lines.add(line.toString());
        }
        return lines;
    }
    private String safe(String value) { return value == null ? "" : value.replace('\u0000', ' '); }
}
