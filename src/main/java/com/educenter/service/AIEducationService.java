package com.educenter.service;

import com.educenter.dto.AIExerciseRequest;
import com.educenter.dto.AIExamRequest;
import com.educenter.dto.AIReportRequest;
import java.util.Map;

public interface AIEducationService {
    Map<Long, String> generateReports(Long teacherUserId, AIReportRequest request);
    String generateExercises(Long teacherUserId, AIExerciseRequest request);
    String generateExam(Long teacherUserId, AIExamRequest request);
}
