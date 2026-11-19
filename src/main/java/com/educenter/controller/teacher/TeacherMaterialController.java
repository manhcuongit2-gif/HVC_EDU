package com.educenter.controller.teacher;

import com.educenter.entity.LearningMaterial;
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
@RequestMapping("/teacher/materials")
public class TeacherMaterialController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TeacherMaterialController.class);

    private final LearningMaterialService materialService;

    public TeacherMaterialController(LearningMaterialService materialService) {
        this.materialService = materialService;
    }

    @GetMapping
    public String index(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        model.addAttribute("currentUser", user);
        try {
            model.addAttribute("materials", materialService.findForTeacher(user.getId()));
        } catch (DataAccessException e) {
            LOGGER.error("Could not load learning materials for teacher user {}", user.getId(), e);
            model.addAttribute("materials", List.of());
            model.addAttribute("errorMessage", "Không thể tải tài liệu lúc này. Vui lòng thử lại sau.");
        }
        return "teacher/materials/list";
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
            materialService.createForTeacher(classId, title, description, externalUrl, file, user.getId());
            redirect.addFlashAttribute("successMessage", "Đã đăng tài liệu cho lớp.");
            return "redirect:/teacher/materials";
        } catch (Exception e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/teacher/materials/add";
        }
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user,
                           Model model, RedirectAttributes redirect) {
        try {
            fillForm(user, model, materialService.findForTeacherById(id, user.getId()));
            return "materials/form";
        } catch (Exception e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/teacher/materials";
        }
    }

    @PostMapping("/edit/{id}")
    public String edit(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user,
                       @RequestParam Long classId, @RequestParam String title,
                       @RequestParam(required = false) String description, @RequestParam(required = false) String externalUrl,
                       @RequestParam(required = false) MultipartFile file, RedirectAttributes redirect) {
        try {
            materialService.updateForTeacher(id, classId, title, description, externalUrl, file, user.getId());
            redirect.addFlashAttribute("successMessage", "Đã cập nhật tài liệu.");
        } catch (Exception e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/teacher/materials";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user,
                         RedirectAttributes redirect) {
        try {
            materialService.deleteForTeacher(id, user.getId());
            redirect.addFlashAttribute("successMessage", "Đã xóa tài liệu.");
        } catch (Exception e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/teacher/materials";
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> download(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        LearningMaterial material = materialService.findFileForTeacher(id, user.getId());
        Resource resource = new FileSystemResource(materialService.resolveStoredFile(material));
        String type = material.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : material.getContentType();
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(type))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(material.getOriginalFilename(), StandardCharsets.UTF_8).build().toString())
                .body(resource);
    }

    private void fillForm(CustomUserDetails user, Model model, LearningMaterial material) {
        model.addAttribute("currentUser", user);
        model.addAttribute("classes", materialService.findTeacherClasses(user.getId()));
        model.addAttribute("material", material);
        model.addAttribute("actionUrl", material == null ? "/teacher/materials/add" : "/teacher/materials/edit/" + material.getId());
        model.addAttribute("returnUrl", "/teacher/materials");
        model.addAttribute("roleLabel", "Giáo viên");
    }
}
