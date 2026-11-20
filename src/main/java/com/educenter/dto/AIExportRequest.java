package com.educenter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AIExportRequest(@Size(max = 160) String title,
                              @NotBlank @Size(max = 50000) String content,
                              @NotBlank String format) {}
