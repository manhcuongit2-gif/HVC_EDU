package com.educenter.service;

import org.springframework.web.multipart.MultipartFile;

public interface AIDocumentService {
    String extractText(MultipartFile file);
}
