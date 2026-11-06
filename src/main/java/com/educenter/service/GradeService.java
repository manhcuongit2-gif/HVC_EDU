package com.educenter.service;

import com.educenter.entity.Grade;

import java.math.BigDecimal;
import java.util.List;

public interface GradeService {
    List<Grade> getGradesByClassId(Long classId);
    List<Grade> getGradesByStudentId(Long studentId);
    void saveGrade(Long classId, Long studentId, BigDecimal testScore, BigDecimal midtermScore, BigDecimal finalScore, String note);
    void saveGradeBatch(Long classId, List<Long> studentIds, List<BigDecimal> testScores, List<BigDecimal> midtermScores, List<BigDecimal> finalScores, List<String> notes);
}
