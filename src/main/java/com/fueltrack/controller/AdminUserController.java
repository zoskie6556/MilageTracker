package com.fueltrack.controller;

import com.fueltrack.dto.AdminUserDetail;
import com.fueltrack.dto.AdminUserSummary;
import com.fueltrack.model.User;
import com.fueltrack.service.AdminUserService;
import com.fueltrack.service.CurrentUserService;
import com.fueltrack.service.ImageStorageService;
import com.fueltrack.service.OdometerReadingService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private static final int PAGE_SIZE = 20;

    private final AdminUserService adminUserService;
    private final CurrentUserService currentUserService;
    private final OdometerReadingService readingService;
    private final ImageStorageService imageStorageService;

    public AdminUserController(
            AdminUserService adminUserService,
            CurrentUserService currentUserService,
            OdometerReadingService readingService,
            ImageStorageService imageStorageService) {
        this.adminUserService = adminUserService;
        this.currentUserService = currentUserService;
        this.readingService = readingService;
        this.imageStorageService = imageStorageService;
    }

    @GetMapping("/admin/users")
    public String users(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        int safePage = Math.max(0, page);
        Page<AdminUserSummary> users = adminUserService.search(q, safePage, PAGE_SIZE);
        model.addAttribute("users", users);
        model.addAttribute("query", q);
        return "admin/users";
    }

    @GetMapping("/admin/users/{userId}")
    public String userDetail(@PathVariable Long userId, Model model) {
        AdminUserDetail detail = adminUserService.getDetail(userId);
        model.addAttribute("userDetail", detail);
        return "admin/user-detail";
    }

    @PostMapping("/admin/users/{userId}/enable")
    public String enable(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long userId,
            RedirectAttributes redirectAttributes) {
        User actor = currentUserService.requireUser(principal.getUsername());
        adminUserService.setEnabled(actor.getId(), userId, true);
        redirectAttributes.addFlashAttribute("accountUpdated", "Account enabled.");
        return "redirect:/admin/users";
    }

    @PostMapping("/admin/users/{userId}/disable")
    public String disable(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long userId,
            RedirectAttributes redirectAttributes) {
        User actor = currentUserService.requireUser(principal.getUsername());
        try {
            adminUserService.setEnabled(actor.getId(), userId, false);
            redirectAttributes.addFlashAttribute("accountUpdated", "Account disabled.");
        } catch (IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("accountError", exception.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/admin/users/{userId}/photos/{filename:.+}")
    public ResponseEntity<ByteArrayResource> userPhoto(
            @PathVariable Long userId,
            @PathVariable String filename) {
        AdminUserDetail detail = adminUserService.getDetail(userId);
        boolean belongsToUser = detail.readings().stream()
                .anyMatch(reading -> filename.equals(reading.photoPath()));
        if (!belongsToUser || !readingService.ownsPhoto(filename, userId)) {
            throw new EntityNotFoundException("Photo not found");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(new ByteArrayResource(imageStorageService.load(filename)));
    }
}
