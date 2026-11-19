package com.educenter.service;

import com.educenter.dto.AdminExcelImportResult;
import com.educenter.dto.AdminExcelImportRow;
import com.educenter.entity.*;
import com.educenter.repository.*;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class AdminExcelImportService {
    private static final Map<String, List<String>> HEADERS = Map.of(
            "parents", List.of("Tên đăng nhập", "Họ và tên", "Email", "Số điện thoại", "Địa chỉ", "Mật khẩu", "Nghề nghiệp", "Ghi chú"),
            "students", List.of("Họ tên học sinh", "Email phụ huynh", "Ngày sinh (yyyy-MM-dd)", "Giới tính", "Số điện thoại", "Trường", "Khối"),
            "classes", List.of("Tên lớp", "Môn học", "Email người dạy", "Sĩ số tối đa", "Học phí", "Phí mỗi buổi", "Mô tả")
    );

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ParentRepository parentRepository;
    private final StudentRepository studentRepository;
    private final ClassRepository classRepository;
    private final ClassStudentRepository classStudentRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transactionTemplate;

    public AdminExcelImportService(UserRepository userRepository, RoleRepository roleRepository,
                                   ParentRepository parentRepository, StudentRepository studentRepository,
                                   ClassRepository classRepository, ClassStudentRepository classStudentRepository,
                                   SubjectRepository subjectRepository, TeacherRepository teacherRepository,
                                   PasswordEncoder passwordEncoder, PlatformTransactionManager transactionManager) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.parentRepository = parentRepository;
        this.studentRepository = studentRepository;
        this.classRepository = classRepository;
        this.classStudentRepository = classStudentRepository;
        this.subjectRepository = subjectRepository;
        this.teacherRepository = teacherRepository;
        this.passwordEncoder = passwordEncoder;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public List<String> types() { return List.of("parents", "students", "classes"); }

    public String displayName(String type) {
        return switch (type) {
            case "parents" -> "Phụ huynh";
            case "students" -> "Học sinh";
            case "classes" -> "Lớp học";
            default -> throw new IllegalArgumentException("Loại dữ liệu không được hỗ trợ.");
        };
    }

    public List<String> headers(String type) {
        List<String> headers = HEADERS.get(type);
        if (headers == null) throw new IllegalArgumentException("Loại dữ liệu không được hỗ trợ.");
        return headers;
    }

    public byte[] createTemplate(String type) {
        List<String> headers = headers(type);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(displayName(type));
            Row headerRow = sheet.createRow(0);
            var headerStyle = workbook.createCellStyle();
            var font = workbook.createFont(); font.setBold(true); font.setColor((short) 9); headerStyle.setFont(font);
            for (int column = 0; column < headers.size(); column++) {
                var cell = headerRow.createCell(column);
                cell.setCellValue(headers.get(column)); cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(column, Math.min(55 * 256, Math.max(18 * 256, (headers.get(column).length() + 4) * 256)));
            }
            sheet.createFreezePane(0, 1);
            var instructions = workbook.createSheet("Hướng dẫn");
            Row row = instructions.createRow(0); row.createCell(0).setCellValue("Giữ nguyên thứ tự cột. Không xóa hàng tiêu đề. Các dòng hợp lệ sẽ được hiển thị xem trước trước khi nhập.");
            row.createCell(1).setCellValue("Tệp tải lên được dùng một lần cho phiên đăng nhập hiện tại; hệ thống không lưu lại tệp Excel.");
            instructions.setColumnWidth(0, 100 * 256); instructions.setColumnWidth(1, 100 * 256);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Không thể tạo tệp mẫu Excel.", e);
        }
    }

    public List<AdminExcelImportRow> preview(String type, MultipartFile file) {
        List<String> headers = headers(type);
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Chọn tệp Excel để tải lên.");
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!filename.endsWith(".xlsx") && !filename.endsWith(".xls")) throw new IllegalArgumentException("Tệp cần có định dạng .xlsx hoặc .xls.");
        if (file.getSize() > 10L * 1024 * 1024) throw new IllegalArgumentException("Tệp Excel không được vượt quá 10 MB.");
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            if (workbook.getNumberOfSheets() == 0) throw new IllegalArgumentException("Tệp Excel không có trang tính.");
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet.getPhysicalNumberOfRows() < 2) throw new IllegalArgumentException("Tệp chưa có dòng dữ liệu.");
            DataFormatter formatter = new DataFormatter(Locale.forLanguageTag("vi-VN"));
            Row headerRow = sheet.getRow(0);
            for (int i = 0; i < headers.size(); i++) {
                String actual = headerRow == null || headerRow.getCell(i) == null ? "" : formatter.formatCellValue(headerRow.getCell(i)).trim();
                if (!headers.get(i).equalsIgnoreCase(actual)) throw new IllegalArgumentException("Sai mẫu Excel. Hãy tải mẫu đúng cho loại " + displayName(type) + ".");
            }
            List<AdminExcelImportRow> result = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                List<String> values = new ArrayList<>();
                boolean hasData = false;
                for (int column = 0; column < headers.size(); column++) {
                    String value = row == null || row.getCell(column) == null ? "" : formatter.formatCellValue(row.getCell(column)).trim();
                    values.add(value); hasData |= !value.isBlank();
                }
                if (!hasData) continue;
                String message = validate(type, values, seen);
                result.add(new AdminExcelImportRow(rowIndex + 1, List.copyOf(values), message == null, message == null ? "Sẵn sàng nhập" : message));
            }
            if (result.isEmpty()) throw new IllegalArgumentException("Không tìm thấy dòng dữ liệu nào trong tệp.");
            return result;
        } catch (IOException e) {
            throw new IllegalArgumentException("Không thể đọc tệp Excel.", e);
        }
    }

    public AdminExcelImportResult importRows(String type, List<AdminExcelImportRow> rows) {
        int succeeded = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();
        for (AdminExcelImportRow row : rows) {
            if (!row.valid()) { failed++; errors.add("Dòng " + row.rowNumber() + ": " + row.message()); continue; }
            try {
                transactionTemplate.execute(status -> { importRow(type, row.values()); return null; });
                succeeded++;
            } catch (RuntimeException e) {
                failed++;
                errors.add("Dòng " + row.rowNumber() + ": " + (e.getMessage() == null ? "Không nhập được dữ liệu." : e.getMessage()));
            }
        }
        return new AdminExcelImportResult(rows.size(), succeeded, failed, List.copyOf(errors));
    }

    private String validate(String type, List<String> v, Set<String> seen) {
        return switch (type) {
            case "parents" -> validateParent(v, seen);
            case "students" -> validateStudent(v, seen);
            case "classes" -> validateClass(v, seen);
            default -> "Loại dữ liệu không được hỗ trợ.";
        };
    }

    private String validateParent(List<String> v, Set<String> seen) {
        if (v.size() < 8 || blank(v, 0) || blank(v, 1) || blank(v, 2) || blank(v, 5)) return "Thiếu tên đăng nhập, họ tên, email hoặc mật khẩu.";
        String email = v.get(2).toLowerCase(Locale.ROOT);
        String username = v.get(0).toLowerCase(Locale.ROOT);
        if (username.length() < 4 || username.length() > 50) return "Tên đăng nhập phải từ 4 đến 50 ký tự.";
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) return "Email không hợp lệ.";
        if (v.get(5).length() < 8) return "Mật khẩu phải có ít nhất 8 ký tự.";
        if (userRepository.existsByEmail(email) || userRepository.existsByUsername(username)) return "Email hoặc tên đăng nhập đã tồn tại.";
        if (!seen.add("parent:" + email) || !seen.add("username:" + username)) return "Email hoặc tên đăng nhập bị lặp trong tệp.";
        return null;
    }

    private String validateStudent(List<String> v, Set<String> seen) {
        if (v.size() < 7 || blank(v, 0) || blank(v, 1)) return "Thiếu họ tên học sinh hoặc email phụ huynh.";
        Optional<Parent> parent = parentByEmail(v.get(1));
        if (parent.isEmpty()) return "Không tìm thấy phụ huynh theo email " + v.get(1) + ".";
        if (!v.get(2).isBlank()) try { LocalDate.parse(v.get(2)); } catch (DateTimeParseException e) { return "Ngày sinh cần theo định dạng yyyy-MM-dd."; }
        String key = "student:" + parent.get().getId() + ":" + v.get(0).toLowerCase(Locale.ROOT);
        if (!seen.add(key) || studentRepository.findByFullNameAndParentId(v.get(0), parent.get().getId()).isPresent()) return "Học sinh cùng tên đã tồn tại trong hồ sơ phụ huynh.";
        return null;
    }

    private String validateClass(List<String> v, Set<String> seen) {
        if (v.size() < 7 || blank(v, 0) || blank(v, 1) || blank(v, 2) || blank(v, 3) || blank(v, 4)) return "Thiếu tên lớp, môn, email người dạy, sĩ số hoặc học phí.";
        if (classRepository.existsByName(v.get(0)) || !seen.add("class:" + v.get(0).toLowerCase(Locale.ROOT))) return "Tên lớp đã tồn tại hoặc bị lặp trong tệp.";
        if (subjectRepository.findByName(v.get(1)).isEmpty()) return "Không tìm thấy môn học: " + v.get(1) + ".";
        if (teacherRepository.findAllByOrderByIdAsc().stream().noneMatch(t -> t.getUser().getEmail().equalsIgnoreCase(v.get(2)) && Boolean.TRUE.equals(t.getUser().getActive()))) return "Không tìm thấy người dạy đang hoạt động theo email.";
        try { if (Integer.parseInt(v.get(3)) < 1) return "Sĩ số tối đa phải lớn hơn 0."; }
        catch (NumberFormatException e) { return "Sĩ số tối đa phải là số nguyên."; }
        try { if (decimal(v.get(4)).signum() < 0) return "Học phí không được âm."; }
        catch (NumberFormatException e) { return "Học phí phải là số."; }
        if (!v.get(5).isBlank()) try { if (decimal(v.get(5)).signum() < 0) return "Phí mỗi buổi không được âm."; }
        catch (NumberFormatException e) { return "Phí mỗi buổi phải là số."; }
        return null;
    }

    private void importRow(String type, List<String> v) {
        switch (type) {
            case "parents" -> importParent(v);
            case "students" -> importStudent(v);
            case "classes" -> importClass(v);
            default -> throw new IllegalArgumentException("Loại dữ liệu không được hỗ trợ.");
        }
    }

    private void importParent(List<String> v) {
        Role role = roleRepository.findByName("ROLE_PARENT").orElseThrow(() -> new IllegalStateException("Chưa cấu hình ROLE_PARENT."));
        User user = new User(); user.setUsername(v.get(0)); user.setFullName(v.get(1)); user.setEmail(v.get(2));
        user.setPhone(emptyToNull(v.get(3))); user.setAddress(emptyToNull(v.get(4))); user.setPassword(passwordEncoder.encode(v.get(5)));
        user.setRole(role); user.setActive(true); user = userRepository.saveAndFlush(user);
        Parent parent = new Parent(); parent.setUser(user); parent.setOccupation(emptyToNull(v.get(6))); parent.setNote(emptyToNull(v.get(7)));
        parentRepository.save(parent);
    }

    private void importStudent(List<String> v) {
        Parent parent = parentByEmail(v.get(1)).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phụ huynh theo email."));
        Student student = new Student(); student.setFullName(v.get(0));
        student.setDateOfBirth(v.get(2).isBlank() ? null : LocalDate.parse(v.get(2)));
        student.setGender(emptyToNull(v.get(3))); student.setPhone(emptyToNull(v.get(4)));
        student.setSchool(emptyToNull(v.get(5))); student.setGradeLevel(emptyToNull(v.get(6)));
        student.setParent(parent); student.setActive(true); studentRepository.save(student);
    }

    private void importClass(List<String> v) {
        Subject subject = subjectRepository.findByName(v.get(1)).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy môn học."));
        Teacher teacher = teacherRepository.findAllByOrderByIdAsc().stream()
                .filter(t -> t.getUser().getEmail().equalsIgnoreCase(v.get(2)) && Boolean.TRUE.equals(t.getUser().getActive())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dạy đang hoạt động."));
        ClassEntity entity = new ClassEntity(); entity.setName(v.get(0)); entity.setSubject(subject); entity.setTeacher(teacher);
        entity.setMaxStudents(Integer.parseInt(v.get(3))); entity.setTuitionFee(decimal(v.get(4)));
        entity.setSessionFee(v.get(5).isBlank() ? null : decimal(v.get(5))); entity.setDescription(emptyToNull(v.get(6))); entity.setStatus("ACTIVE");
        classRepository.save(entity);
    }

    private Optional<Parent> parentByEmail(String email) {
        return userRepository.findByEmail(email).flatMap(user -> parentRepository.findByUserId(user.getId()));
    }

    private BigDecimal decimal(String value) {
        String normalized = value.trim().replace(" ", "");
        if (normalized.contains(",") && normalized.contains(".")) {
            if (normalized.lastIndexOf(',') > normalized.lastIndexOf('.')) normalized = normalized.replace(".", "").replace(',', '.');
            else normalized = normalized.replace(",", "");
        } else if (normalized.matches("\\d{1,3}(,\\d{3})+")) normalized = normalized.replace(",", "");
        else if (normalized.contains(",")) normalized = normalized.replace(',', '.');
        else if (normalized.matches("\\d{1,3}(\\.\\d{3})+")) normalized = normalized.replace(".", "");
        return new BigDecimal(normalized);
    }

    private boolean blank(List<String> values, int index) { return index >= values.size() || values.get(index) == null || values.get(index).isBlank(); }
    private String emptyToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
