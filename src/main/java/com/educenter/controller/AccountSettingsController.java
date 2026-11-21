package com.educenter.controller;

import com.educenter.security.CustomUserDetails;
import com.educenter.service.impl.AccountSettingsService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.file.Path;

@Controller
@RequestMapping("/account")
@PreAuthorize("isAuthenticated()")
public class AccountSettingsController {
    private final AccountSettingsService accountSettings;

    public AccountSettingsController(AccountSettingsService accountSettings) { this.accountSettings = accountSettings; }

    @GetMapping("/settings")
    public String settings(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        model.addAttribute("currentUser", user);
        model.addAttribute("account", user.getUser());
        model.addAttribute("hasAvatar", user.getUser().getAvatarFilename() != null);
        return "account/settings";
    }

    @PostMapping("/profile")
    public String updateProfile(@AuthenticationPrincipal CustomUserDetails currentUser,
                                @RequestParam String fullName, @RequestParam String email,
                                @RequestParam(required = false) String phone,
                                @RequestParam(required = false) String address,
                                RedirectAttributes redirect) {
        try {
            var saved = accountSettings.updateProfile(currentUser.getId(), fullName, email, phone, address);
            currentUser.getUser().setFullName(saved.getFullName());
            currentUser.getUser().setEmail(saved.getEmail());
            currentUser.getUser().setPhone(saved.getPhone());
            currentUser.getUser().setAddress(saved.getAddress());
            redirect.addFlashAttribute("successMessage", "Đã cập nhật thông tin tài khoản.");
        } catch (IllegalArgumentException error) {
            redirect.addFlashAttribute("errorMessage", error.getMessage());
        }
        return "redirect:/account/settings";
    }

    @PostMapping("/password")
    public String changePassword(@AuthenticationPrincipal CustomUserDetails user,
                                 @RequestParam String currentPassword, @RequestParam String newPassword,
                                 @RequestParam String confirmPassword, HttpServletRequest request,
                                 HttpServletResponse response, RedirectAttributes redirect) {
        try {
            accountSettings.updatePassword(user.getId(), currentPassword, newPassword, confirmPassword);
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            new SecurityContextLogoutHandler().logout(request, response, authentication);
            return "redirect:/login?passwordChanged=true";
        } catch (IllegalArgumentException error) {
            redirect.addFlashAttribute("errorMessage", error.getMessage());
            return "redirect:/account/settings#password";
        }
    }

    @PostMapping("/avatar")
    public String updateAvatar(@AuthenticationPrincipal CustomUserDetails user,
                               @RequestParam("avatar") MultipartFile avatar, RedirectAttributes redirect) {
        try {
            String filename = accountSettings.updateAvatar(user.getId(), avatar);
            user.getUser().setAvatarFilename(filename);
            redirect.addFlashAttribute("successMessage", "Đã cập nhật ảnh đại diện.");
        } catch (IllegalArgumentException error) {
            redirect.addFlashAttribute("errorMessage", error.getMessage());
        }
        return "redirect:/account/settings";
    }

    @PostMapping("/avatar/remove")
    public String removeAvatar(@AuthenticationPrincipal CustomUserDetails user, RedirectAttributes redirect) {
        accountSettings.removeAvatar(user.getId());
        user.getUser().setAvatarFilename(null);
        redirect.addFlashAttribute("successMessage", "Đã gỡ ảnh đại diện.");
        return "redirect:/account/settings";
    }

    @GetMapping("/avatar")
    public ResponseEntity<Resource> avatar(@AuthenticationPrincipal CustomUserDetails user) {
        Path image = accountSettings.resolveOwnAvatar(user.getId());
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.noStore().cachePrivate())
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(image));
    }
}
