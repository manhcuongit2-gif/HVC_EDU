package com.educenter.service.impl;

import com.educenter.dto.SessionReportImportPreview;
import com.educenter.dto.SessionReportImportRow;
import com.educenter.entity.MonthlyStudentReport;
import com.educenter.entity.SessionStudentReport;
import com.educenter.entity.ClassSession;
import com.educenter.service.ReportFileService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class ReportFileServiceImpl implements ReportFileService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] SESSION_HEADERS = {"Mã học sinh", "Học sinh", "Điểm danh", "Điểm BTVN", "Nhận xét", "Điểm Cộng", "Ghi chú"};

    @Override
    public byte[] createSessionTemplate(ClassSession session, List<SessionStudentReport> reports) {
        List<List<String>> rows = new ArrayList<>();
        for (SessionStudentReport report : reports) {
            rows.add(List.of(
                    text(report.getStudent().getId()),
                    text(report.getStudent().getFullName()),
                    attendanceLabel(report.getAttendanceStatus()),
                    text(report.getHomeworkScore()),
                    text(report.getComment()),
                    text(report.getGoodEvaluationCount()),
                    text(report.getNote())
            ));
        }
        String sessionInfo = session.getClassEntity().getName() + " · " + session.getWeekdayDisplay() + ", "
                + session.getSessionDate().format(DATE_FORMAT) + " · " + session.getStartTime() + "–" + session.getEndTime();
        byte[] workbook = workbook("Mẫu báo cáo buổi học", sessionInfo, List.of(SESSION_HEADERS), rows);
        try (InputStream input = new java.io.ByteArrayInputStream(workbook);
             Workbook opened = WorkbookFactory.create(input);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = opened.getSheetAt(0);
            DataValidationHelper helper = sheet.getDataValidationHelper();
            DataValidationConstraint constraint = helper.createExplicitListConstraint(new String[]{"Có mặt", "Vắng", "Có phép"});
            int firstDataRow = 4;
            int lastDataRow = Math.max(firstDataRow, firstDataRow + reports.size() - 1);
            DataValidation validation = helper.createValidation(constraint,
                    new CellRangeAddressList(firstDataRow, lastDataRow, 2, 2));
            validation.setShowErrorBox(true);
            validation.createErrorBox("Trạng thái không hợp lệ", "Chọn Có mặt, Vắng hoặc Có phép.");
            sheet.addValidationData(validation);
            opened.write(output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Không thể tạo mẫu Excel báo cáo.", e);
        }
    }

    @Override
    public SessionReportImportPreview parseSessionWorkbook(InputStream inputStream) {
        List<SessionReportImportRow> rows = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            if (workbook.getNumberOfSheets() == 0) return new SessionReportImportPreview(rows, List.of("Tệp Excel không có trang tính."));
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter(Locale.forLanguageTag("vi-VN"));
            int headerRowIndex = findHeaderRow(sheet, formatter);
            if (headerRowIndex < 0) return new SessionReportImportPreview(rows,
                    List.of("Không tìm thấy dòng tiêu đề mẫu: Mã học sinh, Học sinh, Điểm danh, Điểm BTVN, Nhận xét, Điểm Cộng, Ghi chú."));
            Row header = sheet.getRow(headerRowIndex);
            int[] columns = resolveColumns(header, formatter);
            String[] headerNames = {"Mã học sinh", "Học sinh", "Điểm danh", "Điểm BTVN", "Nhận xét", "Điểm Cộng", "Ghi chú"};
            List<String> missingHeaders = new ArrayList<>();
            for (int i = 0; i < columns.length; i++) if (columns[i] < 0) missingHeaders.add(headerNames[i]);
            if (!missingHeaders.isEmpty()) return new SessionReportImportPreview(rows,
                    List.of("Thiếu cột trong mẫu Excel: " + String.join(", ", missingHeaders) + "."));

            for (int i = headerRowIndex + 1; i <= sheet.getLastRowNum(); i++) {
                Row excelRow = sheet.getRow(i);
                if (excelRow == null || isBlankRow(excelRow, formatter)) continue;
                SessionReportImportRow row = new SessionReportImportRow();
                row.setSourceRow(i + 1);
                String id = cell(excelRow, columns[0], formatter);
                String status = cell(excelRow, columns[2], formatter);
                try {
                    row.setStudentId(Long.parseLong(id.trim().replaceAll("\\.0$", "")));
                } catch (RuntimeException e) {
                    errors.add("Dòng " + (i + 1) + ": mã học sinh không hợp lệ.");
                }
                row.setStudentName(cell(excelRow, columns[1], formatter));
                row.setAttendanceStatus(normalizeAttendance(status));
                if (row.getAttendanceStatus() == null) errors.add("Dòng " + (i + 1) + ": điểm danh phải là Có mặt, Vắng hoặc Có phép.");
                row.setHomeworkScore(cell(excelRow, columns[3], formatter).replace(',', '.').trim());
                row.setComment(cell(excelRow, columns[4], formatter).trim());
                row.setGoodEvaluationCount(cell(excelRow, columns[5], formatter).trim());
                if (row.getGoodEvaluationCount().isBlank()) row.setGoodEvaluationCount("0");
                row.setNote(cell(excelRow, columns[6], formatter).trim());
                if (row.getComment().isBlank()) errors.add("Dòng " + (i + 1) + ": nhận xét không được để trống.");
                if (row.getComment().length() > 1000) errors.add("Dòng " + (i + 1) + ": nhận xét tối đa 1000 ký tự.");
                if (row.getNote().length() > 500) errors.add("Dòng " + (i + 1) + ": ghi chú tối đa 500 ký tự.");
                validateNumber(row, errors);
                rows.add(row);
            }
            if (rows.isEmpty()) errors.add("Mẫu Excel chưa có dòng dữ liệu học sinh.");
        } catch (Exception e) {
            errors.add("Không đọc được tệp Excel. Hãy dùng mẫu .xlsx được tải từ hệ thống.");
        }
        return new SessionReportImportPreview(rows, errors);
    }

    @Override
    public byte[] exportSessionExcel(String title, List<SessionStudentReport> reports) {
        List<String> headers = List.of("Ngày", "Học sinh", "Lớp", "Điểm danh", "BTVN", "Nhận xét", "Điểm Cộng", "Ghi chú");
        List<List<String>> rows = reports.stream().map(r -> List.of(
                sessionDate(r), text(r.getStudent().getFullName()), text(r.getClassSession().getClassEntity().getName()),
                attendanceLabel(r.getAttendanceStatus()), text(r.getHomeworkScore()), text(r.getComment()),
                text(r.getGoodEvaluationCount()), text(r.getNote()))).toList();
        return workbook(title, "Báo cáo buổi học", headers, rows);
    }

    @Override
    public byte[] exportMonthlyExcel(String title, List<MonthlyStudentReport> reports) {
        List<String> headers = List.of("Tháng", "Học sinh", "Lớp", "Có mặt", "Vắng", "Có phép", "TB BTVN", "Điểm Cộng", "Nhận xét tháng");
        List<List<String>> rows = reports.stream().map(r -> List.of(
                r.getReportMonth() == null ? "" : r.getReportMonth().format(DateTimeFormatter.ofPattern("MM/yyyy")),
                text(r.getStudent().getFullName()), text(r.getClassEntity().getName()), text(r.getPresentCount()),
                text(r.getAbsentCount()), text(r.getExcusedCount()), text(r.getHomeworkAverage()),
                text(r.getGoodEvaluationCount()), text(r.getTeacherComment()))).toList();
        return workbook(title, "Tổng kết học tập theo tháng", headers, rows);
    }

    @Override
    public byte[] exportSessionPdf(String title, List<SessionStudentReport> reports) {
        List<List<String>> blocks = new ArrayList<>();
        for (SessionStudentReport r : reports) {
            blocks.add(List.of("Ngày: " + sessionDate(r) + " · Lớp: " + text(r.getClassSession().getClassEntity().getName()),
                    "Học sinh: " + text(r.getStudent().getFullName()),
                    "Điểm danh: " + attendanceLabel(r.getAttendanceStatus()) + " · BTVN: " + text(r.getHomeworkScore())
                            + " · Điểm Cộng: " + text(r.getGoodEvaluationCount()),
                    "Nhận xét: " + text(r.getComment()), "Ghi chú: " + text(r.getNote())));
        }
        return pdf(title, blocks);
    }

    @Override
    public byte[] exportMonthlyPdf(String title, List<MonthlyStudentReport> reports) {
        List<List<String>> blocks = new ArrayList<>();
        for (MonthlyStudentReport r : reports) {
            blocks.add(List.of("Tháng: " + (r.getReportMonth() == null ? "" : r.getReportMonth().format(DateTimeFormatter.ofPattern("MM/yyyy")))
                            + " · Lớp: " + text(r.getClassEntity().getName()),
                    "Học sinh: " + text(r.getStudent().getFullName()),
                    "Có mặt: " + text(r.getPresentCount()) + " · Vắng: " + text(r.getAbsentCount()) + " · Có phép: " + text(r.getExcusedCount())
                            + " · TB BTVN: " + text(r.getHomeworkAverage()) + " · Điểm Cộng: " + text(r.getGoodEvaluationCount()),
                    "Nhận xét tháng: " + text(r.getTeacherComment())));
        }
        return pdf(title, blocks);
    }

    private byte[] workbook(String title, String subtitle, List<String> headers, List<List<String>> rows) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Báo cáo");
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont(); titleFont.setBold(true); titleFont.setFontHeightInPoints((short) 16); titleFont.setColor(IndexedColors.DARK_BLUE.getIndex());
            titleStyle.setFont(titleFont);
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont(); headerFont.setBold(true); headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont); headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex()); headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER); headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            Row titleRow = sheet.createRow(0); titleRow.createCell(0).setCellValue(title); titleRow.getCell(0).setCellStyle(titleStyle);
            Row subtitleRow = sheet.createRow(1); subtitleRow.createCell(0).setCellValue(subtitle);
            if (headers.size() > 1) sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, headers.size() - 1));
            int headerIndex = 3;
            Row header = sheet.createRow(headerIndex); header.setHeightInPoints(30);
            for (int c = 0; c < headers.size(); c++) { Cell cell = header.createCell(c); cell.setCellValue(headers.get(c)); cell.setCellStyle(headerStyle); }
            for (int r = 0; r < rows.size(); r++) {
                Row row = sheet.createRow(headerIndex + r + 1);
                List<String> values = rows.get(r);
                for (int c = 0; c < headers.size(); c++) row.createCell(c).setCellValue(c < values.size() ? values.get(c) : "");
            }
            if (!rows.isEmpty()) sheet.setAutoFilter(new CellRangeAddress(headerIndex, headerIndex + rows.size(), 0, headers.size() - 1));
            sheet.createFreezePane(0, headerIndex + 1);
            for (int c = 0; c < headers.size(); c++) sheet.setColumnWidth(c, Math.min(10000, Math.max(3400, headers.get(c).length() * 550)));
            if (headers.size() >= 5) sheet.setColumnWidth(headers.size() - 1, 9500);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Không thể tạo tệp Excel báo cáo.", e);
        }
    }

    private byte[] pdf(String title, List<List<String>> blocks) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream();
             InputStream fontStream = getClass().getResourceAsStream("/fonts/NotoSans-Vietnamese.ttf")) {
            if (fontStream == null) throw new IllegalStateException("Thiếu phông chữ tiếng Việt cho báo cáo PDF.");
            PDType0Font font = PDType0Font.load(document, fontStream);
            List<String> lines = new ArrayList<>();
            for (List<String> block : blocks) {
                for (String line : block) lines.addAll(wrapPdfLine(line, font, 10, 765));
                lines.add("");
            }
            int pageSize = 34;
            int pages = Math.max(1, (int) Math.ceil(lines.size() / (double) pageSize));
            for (int p = 0; p < pages; p++) {
                PDPage page = new PDPage(new PDRectangle(842, 595));
                document.addPage(page);
                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    content.beginText(); content.setFont(font, 16); content.newLineAtOffset(38, 555); content.showText(safePdf(title)); content.endText();
                    content.setStrokingColor(203, 213, 225); content.moveTo(38, 540); content.lineTo(804, 540); content.stroke();
                    int end = Math.min(lines.size(), (p + 1) * pageSize);
                    float y = 520;
                    for (int i = p * pageSize; i < end; i++) {
                        String line = lines.get(i);
                        content.beginText(); content.setFont(font, line.isBlank() ? 6 : 9.5f); content.newLineAtOffset(40, y); content.showText(safePdf(line)); content.endText();
                        y -= line.isBlank() ? 8 : 14;
                    }
                    content.beginText(); content.setFont(font, 8); content.newLineAtOffset(740, 20); content.showText((p + 1) + " / " + pages); content.endText();
                }
            }
            document.save(output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Không thể tạo tệp PDF báo cáo.", e);
        }
    }

    private List<String> wrapPdfLine(String text, PDType0Font font, float fontSize, float maxWidth) throws IOException {
        String value = text == null ? "" : text;
        List<String> wrapped = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : value.split("\\s+")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (font.getStringWidth(candidate) / 1000f * fontSize > maxWidth && !line.isEmpty()) {
                wrapped.add(line.toString()); line.setLength(0); line.append(word);
            } else {
                if (!line.isEmpty()) line.append(' ');
                line.append(word);
            }
        }
        if (!line.isEmpty()) wrapped.add(line.toString());
        if (wrapped.isEmpty()) wrapped.add("");
        return wrapped;
    }

    private int findHeaderRow(Sheet sheet, DataFormatter formatter) {
        for (int i = 0; i <= Math.min(sheet.getLastRowNum(), 12); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            for (Cell cell : row) if (normalize(cell.getColumnIndex() >= 0 ? formatter.formatCellValue(cell) : "").equals("ma hoc sinh")) return i;
        }
        return -1;
    }

    private int[] resolveColumns(Row row, DataFormatter formatter) {
        int[] columns = {-1, -1, -1, -1, -1, -1, -1};
        for (Cell cell : row) {
            String name = normalize(formatter.formatCellValue(cell));
            int index = switch (name) {
                case "ma hoc sinh" -> 0; case "hoc sinh" -> 1; case "diem danh" -> 2; case "diem bt vn", "diem btvn" -> 3;
                case "nhan xet" -> 4; case "diem cong" -> 5; case "ghi chu" -> 6; default -> -1;
            };
            if (index >= 0) columns[index] = cell.getColumnIndex();
        }
        return columns;
    }

    private boolean isBlankRow(Row row, DataFormatter formatter) {
        for (Cell cell : row) if (!formatter.formatCellValue(cell).isBlank()) return false;
        return true;
    }

    private String cell(Row row, int column, DataFormatter formatter) {
        return column < 0 || row.getCell(column) == null ? "" : formatter.formatCellValue(row.getCell(column)).trim();
    }

    private String normalizeAttendance(String value) {
        String normalized = normalize(value);
        return switch (normalized) {
            case "present", "co mat", "di hoc" -> "PRESENT";
            case "absent", "vang" -> "ABSENT";
            case "excused", "co phep" -> "EXCUSED";
            default -> null;
        };
    }

    private String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT).replace('đ', 'd').replaceAll("\\s+", " ").trim();
    }

    private void validateNumber(SessionReportImportRow row, List<String> errors) {
        if (!row.getHomeworkScore().isBlank()) {
            try {
                BigDecimal score = new BigDecimal(row.getHomeworkScore());
                if (score.signum() < 0 || score.compareTo(BigDecimal.TEN) > 0) throw new NumberFormatException();
            } catch (NumberFormatException e) { errors.add("Dòng " + row.getSourceRow() + ": điểm BTVN phải nằm trong khoảng 0–10."); }
        }
        try {
            if (Integer.parseInt(row.getGoodEvaluationCount()) < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) { errors.add("Dòng " + row.getSourceRow() + ": Điểm Cộng phải là số nguyên không âm."); }
    }

    private String sessionDate(SessionStudentReport report) {
        ClassSession session = report.getClassSession();
        return session.getWeekdayDisplay() + ", " + session.getSessionDate().format(DATE_FORMAT);
    }

    private String attendanceLabel(String status) {
        return switch (status == null ? "" : status) {
            case "PRESENT" -> "Có mặt"; case "EXCUSED" -> "Có phép"; case "ABSENT" -> "Vắng"; default -> "";
        };
    }

    private String text(Object value) { return value == null ? "" : value.toString(); }
    private String safePdf(String value) { return value == null ? "" : value.replace('\u0000', ' '); }
}
