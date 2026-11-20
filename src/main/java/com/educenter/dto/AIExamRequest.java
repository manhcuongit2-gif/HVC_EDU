package com.educenter.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AIExamRequest(@NotBlank @Size(max = 80) String subject,
                            @Size(max = 40) String gradeLevel,
                            @NotBlank @Size(max = 180) String scope,
                            @Min(1) @Max(100) int questionCount,
                            @Size(max = 100) String structure,
                            @Size(max = 30) String difficulty,
                            boolean includeAnswerKey,
                            boolean includeExplanation,
                            @Min(1) @Max(180) int minutes) {}
