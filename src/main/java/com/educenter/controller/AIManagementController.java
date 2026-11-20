package com.educenter.controller;

import com.educenter.config.AIProperties;
import com.educenter.dto.AISettingsUpdate;
import com.educenter.entity.AISettings;
import com.educenter.service.AIUsageService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.Arrays;
import java.util.Map;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class AIManagementController {
    private final AIUsageService usageService;
    private final AIProperties properties;

    public AIManagementController(AIUsageService usageService, AIProperties properties) {
        this.usageService = usageService;
        this.properties = properties;
    }

    @GetMapping("/admin/ai-management")
    public String page(Model model) {
        model.addAttribute("settings", usageService.settings());
        model.addAttribute("usage", usageService.adminSummary());
        model.addAttribute("models", Arrays.stream(properties.getGroq().getAllowedModels().split(","))
                .map(String::trim).filter(s -> !s.isBlank()).toList());
        model.addAttribute("apiConfigured", properties.isEnabled() && properties.getGroq().hasApiKey());
        return "admin/ai-management";
    }

    @GetMapping("/api/ai/admin/settings")
    @ResponseBody
    public AISettings getSettings() { return usageService.settings(); }

    @PutMapping("/api/ai/admin/settings")
    @ResponseBody
    public Map<String, Object> updateSettings(@RequestBody AISettingsUpdate update) {
        AISettings saved = usageService.updateSettings(update);
        return Map.of("saved", true, "enabled", saved.isEnabled());
    }

    @GetMapping("/api/ai/admin/usage")
    @ResponseBody
    public Map<String, Object> usage() { return usageService.adminSummary(); }
}
