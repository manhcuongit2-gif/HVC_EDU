package com.educenter.dto;

import com.educenter.entity.ExamBlueprintItem;
import java.util.List;

public record ExamBlueprintPreviewItem(ExamBlueprintItem item, List<StudentExamOptionView> options,
                                       String correctAnswer, String explanation) { }
