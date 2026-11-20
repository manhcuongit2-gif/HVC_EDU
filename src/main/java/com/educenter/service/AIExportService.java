package com.educenter.service;

public interface AIExportService {
    byte[] exportWord(String title, String content);
    byte[] exportPdf(String title, String content);
}
