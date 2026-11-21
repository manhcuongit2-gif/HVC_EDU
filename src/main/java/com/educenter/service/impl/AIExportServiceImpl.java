package com.educenter.service.impl;

import com.educenter.service.AIExportService;
import com.educenter.dto.ExamExportDocument;
import com.educenter.dto.ExamExportQuestion;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.List;

@Service
public class AIExportServiceImpl implements AIExportService {
    private final FormulaRenderer formulaRenderer = new FormulaRenderer();
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

    @Override
    public byte[] exportExamWord(ExamExportDocument exam) {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            XWPFParagraph heading = document.createParagraph();
            heading.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = heading.createRun();
            titleRun.setText(safe(exam.title())); titleRun.setBold(true); titleRun.setFontSize(16);
            XWPFParagraph subtitle = document.createParagraph();
            subtitle.setAlignment(ParagraphAlignment.CENTER);
            subtitle.createRun().setText(safe(exam.subtitle()));
            for (ExamExportQuestion question : exam.questions()) {
                XWPFParagraph prompt = document.createParagraph();
                XWPFRun label = prompt.createRun();
                label.setBold(true); label.setText(question.number() + ". [" + question.points() + " điểm] ");
                appendWordRich(prompt, question.prompt());
                for (ExamExportQuestion.ExamExportOption option : question.options()) {
                    XWPFParagraph choice = document.createParagraph();
                    choice.setIndentationLeft(360);
                    choice.createRun().setText(option.key() + ". ");
                    appendWordRich(choice, option.text());
                }
                if (question.answer() != null && !question.answer().isBlank()) {
                    XWPFParagraph answer = document.createParagraph();
                    answer.createRun().setBold(true);
                    answer.createRun().setText("Đáp án: ");
                    appendWordRich(answer, question.answer());
                }
                if (question.explanation() != null && !question.explanation().isBlank()) {
                    XWPFParagraph explanation = document.createParagraph();
                    explanation.createRun().setBold(true);
                    explanation.createRun().setText("Lời giải: ");
                    appendWordRich(explanation, question.explanation());
                }
            }
            document.write(output);
            return output.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Không thể tạo tệp Word đề thi.", e);
        }
    }

    @Override
    public byte[] exportExamPdf(ExamExportDocument exam) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream();
             InputStream fontStream = getClass().getResourceAsStream("/fonts/NotoSans-Vietnamese.ttf")) {
            if (fontStream == null) throw new IllegalStateException("Thiếu phông chữ tiếng Việt.");
            PDType0Font font = PDType0Font.load(document, fontStream);
            ExamPdfLayout layout = new ExamPdfLayout(document, font);
            layout.text(safe(exam.title()), 15); layout.newLine();
            layout.text(safe(exam.subtitle()), 10); layout.newLine(); layout.newLine();
            for (ExamExportQuestion question : exam.questions()) {
                layout.text(question.number() + ". [" + question.points() + " điểm] ", 11, true);
                layout.rich(question.prompt(), 11); layout.newLine();
                for (ExamExportQuestion.ExamExportOption option : question.options()) {
                    layout.text("   " + option.key() + ". ", 10);
                    layout.rich(option.text(), 10); layout.newLine();
                }
                if (question.answer() != null && !question.answer().isBlank()) {
                    layout.text("Đáp án: ", 10, true); layout.rich(question.answer(), 10); layout.newLine();
                }
                if (question.explanation() != null && !question.explanation().isBlank()) {
                    layout.text("Lời giải: ", 10, true); layout.rich(question.explanation(), 10); layout.newLine();
                }
                layout.newLine();
            }
            layout.close();
            document.save(output);
            return output.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Không thể tạo tệp PDF đề thi.", e);
        }
    }

    private void appendWordRich(XWPFParagraph paragraph, String value) throws Exception {
        for (FormulaRenderer.Segment segment : formulaRenderer.segments(safe(value))) {
            if (!segment.isFormula()) {
                paragraph.createRun().setText(segment.text());
                continue;
            }
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(segment.png()));
            if (image != null) paragraph.createRun().addPicture(new ByteArrayInputStream(segment.png()),
                    XWPFDocument.PICTURE_TYPE_PNG, "formula.png",
                    Units.pixelToEMU(image.getWidth()), Units.pixelToEMU(image.getHeight()));
        }
    }

    private final class ExamPdfLayout {
        private final PDDocument document;
        private final PDType0Font font;
        private PDPageContentStream stream;
        private final float left = 48;
        private final float right = 564;
        private float x = left;
        private float y = 748;

        private ExamPdfLayout(PDDocument document, PDType0Font font) throws IOException {
            this.document = document; this.font = font; nextPage();
        }

        private void text(String value, float size) throws IOException { text(value, size, false); }

        private void text(String value, float size, boolean bold) throws IOException {
            String[] paragraphs = safe(value).split("\\R", -1);
            for (int lineIndex = 0; lineIndex < paragraphs.length; lineIndex++) {
                String paragraph = paragraphs[lineIndex];
                for (String word : paragraph.split("\\s+")) {
                    if (word.isEmpty()) continue;
                    String candidate = x == left ? word : " " + word;
                    float width = font.getStringWidth(candidate) / 1000f * size;
                    if (x + width > right && x > left) newLine();
                    candidate = x == left ? word : " " + word;
                    width = font.getStringWidth(candidate) / 1000f * size;
                    if (x + width > right) {
                        for (int i = 0; i < word.length(); i++) {
                            String character = String.valueOf(word.charAt(i));
                            float charWidth = font.getStringWidth(character) / 1000f * size;
                            if (x + charWidth > right && x > left) newLine();
                            drawText(character, size); x += charWidth;
                        }
                    } else {
                        drawText(candidate, size); x += width;
                    }
                }
                if (lineIndex < paragraphs.length - 1) newLine();
            }
        }

        private void rich(String value, float size) throws IOException {
            for (FormulaRenderer.Segment segment : formulaRenderer.segments(safe(value))) {
                if (!segment.isFormula()) {
                    text(segment.text(), size);
                    continue;
                }
                BufferedImage rendered = ImageIO.read(new ByteArrayInputStream(segment.png()));
                if (rendered == null) continue;
                PDImageXObject image = LosslessFactory.createFromImage(document, rendered);
                float scale = Math.min(0.62f, 500f / Math.max(1, rendered.getWidth()));
                float width = rendered.getWidth() * scale;
                float height = rendered.getHeight() * scale;
                if (x + width > right && x > left) newLine();
                ensureRoom(height + 2);
                stream.drawImage(image, x, y - height + 3, width, height);
                x += width + 2;
            }
        }

        private void drawText(String value, float size) throws IOException {
            ensureRoom(size + 2);
            stream.beginText(); stream.setFont(font, size); stream.newLineAtOffset(x, y);
            stream.showText(value); stream.endText();
        }

        private void newLine() throws IOException {
            x = left; y -= 16; ensureRoom(18);
        }

        private void ensureRoom(float needed) throws IOException {
            if (y - needed < 48) nextPage();
        }

        private void nextPage() throws IOException {
            if (stream != null) stream.close();
            document.addPage(new PDPage(PDRectangle.LETTER));
            stream = new PDPageContentStream(document, document.getPage(document.getNumberOfPages() - 1));
            x = left; y = 748;
        }

        private void close() throws IOException { if (stream != null) stream.close(); }
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
