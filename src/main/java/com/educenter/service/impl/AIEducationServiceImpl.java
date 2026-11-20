package com.educenter.service.impl;

import com.educenter.dto.AIExerciseRequest;
import com.educenter.dto.AIExamRequest;
import com.educenter.dto.AIReportRequest;
import com.educenter.entity.Grade;
import com.educenter.entity.SessionStudentReport;
import com.educenter.repository.GradeRepository;
import com.educenter.repository.SessionStudentReportRepository;
import com.educenter.service.AIContextService;
import com.educenter.service.AIEducationService;
import com.educenter.service.AIService;
import com.educenter.service.StudentReportService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;

@Service
public class AIEducationServiceImpl implements AIEducationService {
    private final StudentReportService studentReportService;
    private final SessionStudentReportRepository reportRepository;
    private final GradeRepository gradeRepository;
    private final AIService aiService;
    private final AIContextService contextService;
    private final ObjectMapper objectMapper;

    public AIEducationServiceImpl(StudentReportService studentReportService,
                                  SessionStudentReportRepository reportRepository,
                                  GradeRepository gradeRepository, AIService aiService,
                                  AIContextService contextService, ObjectMapper objectMapper) {
        this.studentReportService = studentReportService;
        this.reportRepository = reportRepository;
        this.gradeRepository = gradeRepository;
        this.aiService = aiService;
        this.contextService = contextService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<Long, String> generateReports(Long teacherUserId, AIReportRequest request) {
        var session = studentReportService.getTeacherSession(request.sessionId(), teacherUserId);
        List<SessionStudentReport> reports = studentReportService.getTeacherSessionReports(request.sessionId(), teacherUserId);
        if (request.studentId() != null) reports = reports.stream()
                .filter(r -> r.getStudent().getId().equals(request.studentId())).toList();
        if (reports.isEmpty()) throw new IllegalArgumentException("Không có học sinh phù hợp để tạo nhận xét.");

        List<Map<String, Object>> inputs = new ArrayList<>();
        Map<String, Long> idsByAlias = new LinkedHashMap<>();
        int index = 1;
        for (SessionStudentReport report : reports) {
            String alias = "HOC_SINH_" + index++;
            idsByAlias.put(alias, report.getStudent().getId());
            Map<String, Object> current = new LinkedHashMap<>();
            current.put("student", alias);
            current.put("attendance", report.getAttendanceStatus());
            current.put("homeworkScore", report.getHomeworkScore() == null ? "Không có dữ liệu" : report.getHomeworkScore());
            current.put("positiveEvaluationCount", report.getGoodEvaluationCount());
            gradeRepository.findByClassEntityIdAndStudentId(session.getClassEntity().getId(), report.getStudent().getId())
                    .ifPresent(grade -> current.put("grade", gradeSummary(grade)));
            current.put("previousSessions", reportRepository.findAll().stream()
                    .filter(previous -> previous.getStudent().getId().equals(report.getStudent().getId())
                            && previous.getClassSession().getClassEntity().getId().equals(session.getClassEntity().getId())
                            && previous.getClassSession().getSessionDate().isBefore(session.getSessionDate()))
                    .sorted(Comparator.comparing((SessionStudentReport previous) -> previous.getClassSession().getSessionDate()).reversed())
                    .limit(3)
                    .map(previous -> Map.of("attendance", previous.getAttendanceStatus(),
                            "homeworkScore", previous.getHomeworkScore() == null ? "Không có dữ liệu" : previous.getHomeworkScore(),
                            "positiveEvaluationCount", previous.getGoodEvaluationCount())).toList());
            inputs.add(current);
        }
        String additional = request.additionalContext() == null ? "" : request.additionalContext().trim();
        additional = contextService.sanitizeText(teacherUserId, "ROLE_TEACHER", session.getClassEntity().getId(), additional);
        String prompt = "Tạo nhận xét ngắn cho từng học sinh dựa CHỈ trên JSON. Không suy diễn hành vi hay đặc điểm không có dữ liệu. "
                + "Trả duy nhất JSON object ánh xạ mã học sinh sang một chuỗi gồm Ưu điểm, Nhược điểm, Giải pháp cải thiện. "
                + "Nếu một mục không đủ dữ liệu, ghi rõ chưa đủ dữ liệu. Không đưa tên thật. Dữ liệu: "
                + writeJson(inputs) + "\nBổ sung của người dạy: " + additional;
        String system = "Bạn là trợ lý soạn nhận xét học tập cho người dạy. Nội dung phải công bằng, ngắn gọn, dựa đúng dữ liệu, không chẩn đoán hay gán tính cách.";
        String generated = aiService.generateText(teacherUserId, "ROLE_TEACHER", system, prompt);
        Map<String, String> byAlias = parseReportJson(generated, idsByAlias.keySet());
        Map<Long, String> result = new LinkedHashMap<>();
        idsByAlias.forEach((alias, studentId) -> {
            String text = byAlias.get(alias);
            if (text != null && !text.isBlank()) result.put(studentId, text.trim());
        });
        if (result.isEmpty()) throw new IllegalStateException("AI không trả về nhận xét ở định dạng hợp lệ.");
        return result;
    }

    @Override
    public String generateExercises(Long teacherUserId, AIExerciseRequest request) {
        String prompt = "Soạn " + request.count() + " bài tập môn " + request.subject() + ", khối "
                + safe(request.gradeLevel()) + ", chủ đề " + request.topic() + ". Nội dung kiến thức: "
                + safe(request.knowledge()) + ". Mức độ: " + safe(request.difficulty()) + ". Thời gian: "
                + request.minutes() + " phút. Yêu cầu bổ sung: " + safe(request.extraRequirements())
                + ". Phân loại bài theo Nhận biết, Thông hiểu, Vận dụng, Vận dụng cao khi phù hợp. Đưa đáp án/hướng dẫn ngắn sau phần bài tập.";
        return aiService.generateText(teacherUserId, "ROLE_TEACHER",
                "Bạn hỗ trợ người dạy soạn tài liệu học tập bằng tiếng Việt. Nội dung cần phù hợp khối lớp và chủ đề; nếu đề bài thiếu kiến thức cụ thể, nêu rõ giả định.", prompt);
    }

    @Override
    public String generateExam(Long teacherUserId, AIExamRequest request) {
        String prompt = "Tạo đề kiểm tra môn " + request.subject() + ", khối " + safe(request.gradeLevel())
                + ", phạm vi " + request.scope() + ", thời gian " + request.minutes() + " phút, "
                + request.questionCount() + " câu. Cấu trúc: " + safe(request.structure()) + ". Độ khó: "
                + safe(request.difficulty()) + ". " + (request.includeAnswerKey() ? "Kèm đáp án." : "Không kèm đáp án.")
                + " " + (request.includeExplanation() ? "Kèm hướng dẫn giải chi tiết." : "Không cần lời giải chi tiết.")
                + " Phân bổ điểm đủ tổng 10, hiển thị rõ điểm từng câu. Kết thúc bằng lời nhắc người dạy kiểm tra lại đề và đáp án.";
        return aiService.generateText(teacherUserId, "ROLE_TEACHER",
                "Bạn hỗ trợ soạn đề bằng tiếng Việt. Không bịa tiêu chuẩn/kiến thức ngoài phạm vi đã nêu; chỉ tạo tài liệu nháp cần được người dạy kiểm tra.", prompt);
    }

    private Map<String, Object> gradeSummary(Grade grade) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("test", grade.getTestScore());
        result.put("midterm", grade.getMidtermScore());
        result.put("final", grade.getFinalScore());
        result.put("average", grade.getAverageScore());
        return result;
    }

    private Map<String, String> parseReportJson(String text, Set<String> aliases) {
        String body = text == null ? "" : text.trim().replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        int start = body.indexOf('{'), end = body.lastIndexOf('}');
        if (start < 0 || end <= start) return Map.of();
        try {
            JsonNode node = objectMapper.readTree(body.substring(start, end + 1));
            Map<String, String> result = new LinkedHashMap<>();
            aliases.forEach(alias -> { if (node.hasNonNull(alias)) result.put(alias, node.path(alias).asText()); });
            return result;
        } catch (Exception e) { return Map.of(); }
    }

    private String writeJson(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (Exception e) { throw new IllegalStateException("Không thể chuẩn bị dữ liệu nhận xét."); }
    }
    private String safe(String value) { return value == null || value.isBlank() ? "chưa cung cấp" : value.trim(); }
}
