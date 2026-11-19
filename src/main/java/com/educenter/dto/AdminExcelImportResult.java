package com.educenter.dto;

import java.util.List;

public record AdminExcelImportResult(int total, int succeeded, int failed, List<String> errors) {}
