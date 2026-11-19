package com.educenter.dto;

import java.util.List;

public record AdminExcelImportRow(int rowNumber, List<String> values, boolean valid, String message) {}
