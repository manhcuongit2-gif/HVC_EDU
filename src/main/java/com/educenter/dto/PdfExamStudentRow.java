package com.educenter.dto;

import com.educenter.entity.ExamAssignment;
import com.educenter.entity.ExamPdfSubmission;
import java.util.List;

public record PdfExamStudentRow(ExamAssignment assignment, ExamPdfSubmission submission, List<ExamPdfSubmission> history,
                                String status, boolean canSubmit) { }
