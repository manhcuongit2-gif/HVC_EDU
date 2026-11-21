package com.educenter.dto;

import java.util.List;

public record ExamExportDocument(String title, String subtitle, List<ExamExportQuestion> questions) { }
