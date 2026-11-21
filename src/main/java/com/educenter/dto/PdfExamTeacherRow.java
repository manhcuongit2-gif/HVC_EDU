package com.educenter.dto;

import com.educenter.entity.ExamPdfSubmission;
import com.educenter.entity.Student;

public record PdfExamTeacherRow(Student student, ExamPdfSubmission submission) { }
