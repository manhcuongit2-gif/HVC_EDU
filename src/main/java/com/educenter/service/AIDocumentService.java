package com.educenter.service;

import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface AIDocumentService {
    String extractText(MultipartFile file);
    List<DocumentPart> extractParts(byte[] bytes, String filename);

    record DocumentPart(String text, Integer pageNumber, String sectionName) { }
}
