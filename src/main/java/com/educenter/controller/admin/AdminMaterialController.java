package com.educenter.controller.admin;

import com.educenter.entity.LearningMaterial;
import com.educenter.repository.ClassRepository;
import com.educenter.security.CustomUserDetails;
import com.educenter.service.LearningMaterialService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
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

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequestMapping("/admin/materials")
public class AdminMaterialController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminMaterialController.class);

    private final LearningMaterialService materialService;
    private final ClassRepository classRepository;

    public AdminMaterialController(LearningMaterialService materialService, ClassRepository classRepository) {
        this.materialService = materialService;
        this.classRepository = classRepository;
    }

    @GetMapping
    public String index(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        model.addAttribute("currentUser", user);
        try {
            model.addAttribute("materials", materialService.findAllForAdmin());
        } catch (DataAccessException e) {
            LOGGER.error("Could not load learning materials for admin", e);
            model.addAttribute("materials", List.of());
            model.addAttribute("errorMessage", "Không thể tải tài liệu lúc này. Vui lòng thử lại sau.");
        }
        return "admin/materials/list";
    }

    @GetMapping("/add")
    public String addForm(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        fillForm(user, model, null);
        return "materials/form";
    }

    @PostMapping("/add")
    public String add(@AuthenticationPrincipal CustomUserDetails user, @RequestParam Long classId,
                      @RequestParam String title, @RequestParam(required = false) String description,
                      @RequestParam(required = false) String externalUrl,
                      @RequestParam(required = false) MultipartFile file, RedirectAttributes redirect) {
        try {
            materialService.createForAdmin(classId, title, description, externalUrl, file, user.getId());
            redirect.addFlashAttribute("successMessage", "Đã thêm tài liệu.");
            return "redirect:/admin/materials";
        } catch (Exception e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/materials/add";
        }
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user,
                           Model model, RedirectAttributes redirect) {
        try {
            fillForm(user, model, materialService.findForAdminById(id));
            return "materials/form";
        } catch (Exception e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/materials";
        }
    }

    @PostMapping("/edit/{id}")
    public String edit(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user,
                       @RequestParam Long classId, @RequestParam String title,
                       @RequestParam(required = false) String description, @RequestParam(required = false) String externalUrl,
                       @RequestParam(required = false) MultipartFile file, RedirectAttributes redirect) {
        try {
            materialService.updateForAdmin(id, classId, title, description, externalUrl, file, user.getId());
            redirect.addFlashAttribute("successMessage", "Đã cập nhật tài liệu.");
        } catch (Exception e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/materials";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            materialService.deleteForAdmin(id);
            redirect.addFlashAttribute("successMessage", "Đã xóa tài liệu.");
        } catch (Exception e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/materials";
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        return fileResponse(materialService.findFileForAdmin(id));
    }

    private void fillForm(CustomUserDetails user, Model model, LearningMaterial material) {
        model.addAttribute("currentUser", user);
        model.addAttribute("classes", classRepository.findAllByOrderByIdDesc());
        model.addAttribute("material", material);
        model.addAttribute("actionUrl", material == null ? "/admin/materials/add" : "/admin/materials/edit/" + material.getId());
        model.addAttribute("returnUrl", "/admin/materials");
        model.addAttribute("roleLabel", "Quản trị viên");
    }

    private ResponseEntity<Resource> fileResponse(LearningMaterial material) {
        Resource resource = new FileSystemResource(materialService.resolveStoredFile(material));
        String type = material.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : material.getContentType();
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(type))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(material.getOriginalFilename(), StandardCharsets.UTF_8).build().toString())
                .body(resource);
    }
}
