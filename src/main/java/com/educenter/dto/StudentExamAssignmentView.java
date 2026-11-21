package com.educenter.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StudentExamAssignmentView(Long id, String title, String scope, String className, String subject, String studentName,
                                        LocalDateTime opensAt, LocalDateTime closesAt, int durationMinutes,
                                        int attemptsUsed, int maxAttempts, String status,
                                        BigDecimal officialScore, BigDecimal totalPoints, String teacherFeedback) {
    public String getStatusLabel() {
        return switch (status) {
            case "OPEN" -> "Đang mở";
            case "UPCOMING" -> "Sắp mở";
            case "IN_PROGRESS" -> "Đang làm";
            case "WAITING_RESULT" -> "Chờ giáo viên chấm";
            case "RESULT_PUBLISHED" -> "Đã công bố";
            case "CLOSED" -> "Đã đóng / hết lượt";
            default -> "Chưa xác định";
        };
    }
}
