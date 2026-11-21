package com.educenter.controller.admin;

import com.educenter.dto.AdminExcelImportResult;
import com.educenter.dto.AdminExcelImportRow;
import com.educenter.security.CustomUserDetails;
import com.educenter.service.AdminExcelImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Controller
@RequestMapping("/admin/import")
public class AdminExcelImportController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminExcelImportController.class);
    private static final String SESSION_KEY = "pendingAdminExcelImports";
    private final AdminExcelImportService importService;

    public AdminExcelImportController(AdminExcelImportService importService) {
        this.importService = importService;
    }

    @GetMapping
    public String index(@AuthenticationPrincipal CustomUserDetails user,
                        @RequestParam(required = false) String type, Model model) {
        model.addAttribute("currentUser", user);
        model.addAttribute("previewType", Set.of("parents", "students", "classes").contains(type) ? type : "parents");
        return "admin/import";
    }

    @GetMapping("/template/{type}.xlsx")
    public ResponseEntity<ByteArrayResource> template(@PathVariable String type) {
        byte[] bytes = importService.createTemplate(type);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("mau-nhap-" + type + ".xlsx", StandardCharsets.UTF_8).build().toString())
                .body(new ByteArrayResource(bytes));
    }

    @PostMapping("/preview")
    public String preview(@RequestParam String type, @RequestParam MultipartFile file,
                          @AuthenticationPrincipal CustomUserDetails user, HttpSession session, Model model) {
        addBaseModel(user, model);
        model.addAttribute("previewType", Set.of("parents", "students", "classes").contains(type) ? type : "parents");
        try {
            List<AdminExcelImportRow> rows = importService.preview(type, file);
            String importId = UUID.randomUUID().toString();
            Map<String, PendingImport> pending = pendingImports(session);
            if (pending.size() >= 3) pending.remove(pending.keySet().iterator().next());
            pending.put(importId, new PendingImport(type, rows));
            model.addAttribute("previewRows", rows);
            model.addAttribute("previewType", type);
            model.addAttribute("previewLabel", importService.displayName(type));
            model.addAttribute("validRowCount", rows.stream().filter(AdminExcelImportRow::valid).count());
            model.addAttribute("importId", importId);
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Could not preview admin Excel import type {}", type, e);
            model.addAttribute("errorMessage", "Không thể đọc tệp Excel. Hãy kiểm tra mẫu và thử lại.");
        }
        return "admin/import";
    }

    @PostMapping("/confirm")
    public String confirm(@RequestParam String importId, HttpSession session,
                          RedirectAttributes redirect) {
        PendingImport pending = pendingImports(session).remove(importId);
        if (pending == null) {
            redirect.addFlashAttribute("errorMessage", "Phiên xem trước đã hết hạn. Hãy tải lại tệp Excel.");
            return "redirect:/admin/import";
        }
        try {
            AdminExcelImportResult result = importService.importRows(pending.type(), pending.rows());
            redirect.addFlashAttribute("importResult", result);
            redirect.addFlashAttribute("importTypeLabel", importService.displayName(pending.type()));
        } catch (Exception e) {
            LOGGER.error("Could not confirm admin Excel import {}", pending.type(), e);
            redirect.addFlashAttribute("errorMessage", "Không thể hoàn tất nhập dữ liệu. Vui lòng kiểm tra cấu hình dữ liệu.");
        }
        return "redirect:/admin/import";
    }

    private void addBaseModel(CustomUserDetails user, Model model) {
        model.addAttribute("currentUser", user);
    }

    @SuppressWarnings("unchecked")
    private Map<String, PendingImport> pendingImports(HttpSession session) {
        Object existing = session.getAttribute(SESSION_KEY);
        if (existing instanceof Map<?, ?>) return (Map<String, PendingImport>) existing;
        Map<String, PendingImport> created = new LinkedHashMap<>();
        session.setAttribute(SESSION_KEY, created);
        return created;
    }

    private record PendingImport(String type, List<AdminExcelImportRow> rows) {}
}
