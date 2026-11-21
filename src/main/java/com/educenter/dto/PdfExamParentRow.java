package com.educenter.dto;

import com.educenter.entity.ExamAssignment;
import com.educenter.entity.ExamPdfSubmission;
import com.educenter.entity.Student;

public record PdfExamParentRow(Student student, ExamAssignment assignment, ExamPdfSubmission submission) { }
