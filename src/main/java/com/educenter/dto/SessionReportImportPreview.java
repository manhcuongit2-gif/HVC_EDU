package com.educenter.dto;

import java.util.List;

public record SessionReportImportPreview(List<SessionReportImportRow> rows, List<String> errors) {
    public boolean isValid() { return errors == null || errors.isEmpty(); }
}
