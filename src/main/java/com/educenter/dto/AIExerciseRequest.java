package com.educenter.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AIExerciseRequest(@NotBlank @Size(max = 80) String subject,
                                @Size(max = 40) String gradeLevel,
                                @NotBlank @Size(max = 180) String topic,
                                @Size(max = 4000) String knowledge,
                                @Min(1) @Max(20) int count,
                                @Size(max = 30) String difficulty,
                                @Min(1) @Max(240) int minutes,
                                @Size(max = 1000) String extraRequirements) {}
