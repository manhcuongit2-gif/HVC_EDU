package com.educenter.controller.parent;

import com.educenter.entity.LearningMaterial;
import com.educenter.security.CustomUserDetails;
import com.educenter.service.LearningMaterialService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/parent/materials")
public class ParentMaterialController {

    private final LearningMaterialService materialService;

    public ParentMaterialController(LearningMaterialService materialService) {
        this.materialService = materialService;
    }

    @GetMapping
    public String index(@AuthenticationPrincipal CustomUserDetails user,
                        @RequestParam(required = false) Long classId, Model model) {
        try {
            model.addAttribute("currentUser", user);
            model.addAttribute("classes", materialService.findParentClasses(user.getId()));
            model.addAttribute("materials", materialService.findForParent(user.getId(), classId));
            model.addAttribute("selectedClassId", classId);
            return "parent/materials";
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> download(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        try {
            return fileResponse(materialService.findFileForParent(id, user.getId()));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    private ResponseEntity<Resource> fileResponse(LearningMaterial material) {
        try {
            Resource resource = new FileSystemResource(materialService.resolveStoredFile(material));
            String type = material.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : material.getContentType();
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(type))
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                            .filename(material.getOriginalFilename(), StandardCharsets.UTF_8).build().toString())
                    .body(resource);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
