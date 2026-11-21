package com.educenter.dto;

import java.nio.file.Path;

public record SecureExamFile(Path path, String contentType, String originalFilename) { }
