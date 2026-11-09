package com.fueltrack.controller;

import com.fueltrack.dto.ConfirmReadingForm;
import com.fueltrack.dto.ManualReadingForm;
import com.fueltrack.dto.PendingPhoto;
import com.fueltrack.dto.PendingReading;
import com.fueltrack.dto.PhotoUploadForm;
import com.fueltrack.dto.ReadingEditForm;
import com.fueltrack.model.DayMoment;
import com.fueltrack.model.OdometerReading;
import com.fueltrack.model.ReadingSource;
import com.fueltrack.model.User;
import com.fueltrack.service.AiReadingResult;
import com.fueltrack.service.CurrentUserService;
import com.fueltrack.service.ImageStorageService;
import com.fueltrack.service.ImageUploadException;
import com.fueltrack.service.OdometerReadingService;
import com.fueltrack.service.PhotoAnalysisService;
import com.fueltrack.service.PhotoAnalysisService.PhotoAnalysis;
import com.fueltrack.service.ReadingWarning;
import com.fueltrack.service.VehicleService;
import com.fueltrack.service.WarningConfirmationRequiredException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Controller
public class ReadingController {

    private static final String PENDING_READING = "pendingReading";
    private static final String PENDING_PHOTO = "pendingPhoto";
    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Africa/Gaborone");

    private final CurrentUserService currentUserService;
    private final VehicleService vehicleService;
    private final OdometerReadingService readingService;
    private final PhotoAnalysisService photoAnalysisService;
    private final ImageStorageService imageStorageService;
    private final Clock clock;

    public ReadingController(
            CurrentUserService currentUserService,
            VehicleService vehicleService,
            OdometerReadingService readingService,
            PhotoAnalysisService photoAnalysisService,
            ImageStorageService imageStorageService,
            Clock clock) {
        this.currentUserService = currentUserService;
        this.vehicleService = vehicleService;
        this.readingService = readingService;
        this.photoAnalysisService = photoAnalysisService;
        this.imageStorageService = imageStorageService;
        this.clock = clock;
    }

    @GetMapping("/readings/new")
    public String addReading(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = currentUserService.requireUser(principal.getUsername());
        model.addAttribute("vehicles", vehicleService.findVehicles(user.getId()));
        return "readings/add";
    }

    @GetMapping("/readings/new/photo")
    public String photoForm(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = currentUserService.requireUser(principal.getUsername());
        PhotoUploadForm form = new PhotoUploadForm();
        form.setTakenAt(LocalDateTime.now(clock.withZone(DISPLAY_ZONE)));
        form.setDayMoment(DayMoment.START_OF_DAY);
        model.addAttribute("photoUploadForm", form);
        model.addAttribute("vehicles", vehicleService.findVehicles(user.getId()));
        return "readings/photo";
    }

    @PostMapping("/readings/photo")
    public String analyzePhoto(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @ModelAttribute("photoUploadForm") PhotoUploadForm form,
            BindingResult bindingResult,
            HttpSession session,
            Model model) {
        User user = currentUserService.requireUser(principal.getUsername());
        if (bindingResult.hasErrors()) {
            addVehicles(model, user);
            return "readings/photo";
        }
        if (form.getPhoto() == null || form.getPhoto().isEmpty()) {
            bindingResult.rejectValue("photo", "required", "Choose an odometer photo.");
            addVehicles(model, user);
            return "readings/photo";
        }
        try {
            vehicleService.requireOwnedVehicle(form.getVehicleId(), user.getId());
        } catch (EntityNotFoundException exception) {
            bindingResult.rejectValue("vehicleId", "notOwned", "Choose one of your vehicles.");
            addVehicles(model, user);
            return "readings/photo";
        }

        try {
            session.removeAttribute(PENDING_READING);
            session.removeAttribute(PENDING_PHOTO);
            PhotoAnalysis analysis = photoAnalysisService.analyze(form.getPhoto());
            if (!analysis.succeeded()) {
                session.setAttribute(PENDING_PHOTO, new PendingPhoto(
                        user.getId(), form.getVehicleId(), analysis.photoPath(), form.getTakenAt(), form.getDayMoment(),
                        form.getNote(), analysis.failureMessage()));
                return "redirect:/readings/new/manual?aiFailure";
            }

            AiReadingResult result = analysis.result();
            session.setAttribute(PENDING_READING, new PendingReading(
                    user.getId(), form.getVehicleId(), analysis.photoPath(), result.odometerKm(), result.odometerKm(),
                    form.getTakenAt(), form.getDayMoment(), form.getNote(), ReadingSource.AI,
                    result.confidence(), result.notes()));
            return "redirect:/readings/confirm";
        } catch (ImageUploadException exception) {
            model.addAttribute("photoError", exception.getMessage());
            addVehicles(model, user);
            return "readings/photo";
        }
    }

    @GetMapping("/readings/new/manual")
    public String manualForm(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(required = false) String aiFailure,
            HttpSession session,
            Model model) {
        User user = currentUserService.requireUser(principal.getUsername());
        ManualReadingForm form = new ManualReadingForm();
        form.setTakenAt(LocalDateTime.now(clock.withZone(DISPLAY_ZONE)));
        form.setDayMoment(DayMoment.START_OF_DAY);
        PendingPhoto pendingPhoto = (PendingPhoto) session.getAttribute(PENDING_PHOTO);
        if (pendingPhoto != null && !pendingPhoto.ownerId().equals(user.getId())) {
            session.removeAttribute(PENDING_PHOTO);
            pendingPhoto = null;
        }
        if (pendingPhoto != null) {
            form.setVehicleId(pendingPhoto.vehicleId());
            form.setTakenAt(pendingPhoto.takenAt());
            form.setDayMoment(pendingPhoto.dayMoment());
            form.setNote(pendingPhoto.note());
            model.addAttribute("photoPath", pendingPhoto.photoPath());
            if (aiFailure != null) {
                model.addAttribute("aiFailureMessage", pendingPhoto.failureMessage());
            }
        }
        model.addAttribute("manualReadingForm", form);
        addVehicles(model, user);
        return "readings/manual";
    }

    @PostMapping("/readings/new/manual")
    public String startManualConfirmation(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @ModelAttribute("manualReadingForm") ManualReadingForm form,
            BindingResult bindingResult,
            HttpSession session,
            Model model) {
        User user = currentUserService.requireUser(principal.getUsername());
        PendingPhoto pendingPhoto = (PendingPhoto) session.getAttribute(PENDING_PHOTO);
        if (pendingPhoto != null && !pendingPhoto.ownerId().equals(user.getId())) {
            session.removeAttribute(PENDING_PHOTO);
            pendingPhoto = null;
        }
        if (pendingPhoto != null && !pendingPhoto.vehicleId().equals(form.getVehicleId())) {
            bindingResult.rejectValue("vehicleId", "photoVehicle",
                    "Choose the vehicle associated with the attached photo.");
        }
        if (!bindingResult.hasErrors()) {
            try {
                vehicleService.requireOwnedVehicle(form.getVehicleId(), user.getId());
            } catch (EntityNotFoundException exception) {
                bindingResult.rejectValue("vehicleId", "notOwned", "Choose one of your vehicles.");
            }
        }
        if (bindingResult.hasErrors()) {
            addVehicles(model, user);
            if (pendingPhoto != null) {
                model.addAttribute("photoPath", pendingPhoto.photoPath());
                model.addAttribute("aiFailureMessage", pendingPhoto.failureMessage());
            }
            return "readings/manual";
        }

        String photoPath = pendingPhoto == null ? null : pendingPhoto.photoPath();
        PendingReading pending = new PendingReading(
                user.getId(), form.getVehicleId(), photoPath, null, form.getReadingKm(), form.getTakenAt(),
                form.getDayMoment(), form.getNote(), ReadingSource.MANUAL, null, null);
        session.setAttribute(PENDING_READING, pending);
        session.removeAttribute(PENDING_PHOTO);
        return "redirect:/readings/confirm";
    }

    @GetMapping("/readings/confirm")
    public String confirmForm(
            @AuthenticationPrincipal UserDetails principal,
            HttpSession session,
            Model model) {
        User user = currentUserService.requireUser(principal.getUsername());
        PendingReading pending = (PendingReading) session.getAttribute(PENDING_READING);
        if (pending != null && !pending.ownerId().equals(user.getId())) {
            session.removeAttribute(PENDING_READING);
            pending = null;
        }
        if (pending == null) {
            return "redirect:/readings/new";
        }
        ConfirmReadingForm form = toForm(pending);
        model.addAttribute("confirmReadingForm", form);
        addConfirmOptions(model, user, pending, form);
        return "readings/confirm";
    }

    @PostMapping("/readings/confirm")
    public String saveConfirmed(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @ModelAttribute("confirmReadingForm") ConfirmReadingForm form,
            BindingResult bindingResult,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        User user = currentUserService.requireUser(principal.getUsername());
        PendingReading pending = (PendingReading) session.getAttribute(PENDING_READING);
        if (pending != null && !pending.ownerId().equals(user.getId())) {
            session.removeAttribute(PENDING_READING);
            pending = null;
        }
        if (pending == null) {
            return "redirect:/readings/new";
        }
        if (bindingResult.hasErrors()) {
            addConfirmOptions(model, user, pending, form);
            return "readings/confirm";
        }

        try {
            readingService.saveConfirmed(user.getId(), pending, form);
        } catch (WarningConfirmationRequiredException exception) {
            model.addAttribute("warnings", exception.getWarnings());
            addConfirmOptions(model, user, pending, form);
            return "readings/confirm";
        } catch (EntityNotFoundException exception) {
            bindingResult.rejectValue("vehicleId", "notOwned", "Choose one of your vehicles.");
            addConfirmOptions(model, user, pending, form);
            return "readings/confirm";
        }

        session.removeAttribute(PENDING_READING);
        session.removeAttribute(PENDING_PHOTO);
        redirectAttributes.addFlashAttribute("readingSaved", true);
        return "redirect:/readings";
    }

    @GetMapping("/readings")
    public String history(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = currentUserService.requireUser(principal.getUsername());
        model.addAttribute("readings", readingService.findHistory(user.getId()));
        return "readings/history";
    }

    @GetMapping("/readings/{readingId}/edit")
    public String editForm(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long readingId,
            Model model) {
        User user = currentUserService.requireUser(principal.getUsername());
        OdometerReading reading = readingService.getOwnedReading(readingId, user.getId());
        ReadingEditForm form = new ReadingEditForm();
        form.setVehicleId(reading.getVehicle().getId());
        form.setReadingKm(reading.getReadingKm().intValueExact());
        form.setTakenAt(reading.getTakenAt().atZone(DISPLAY_ZONE).toLocalDateTime());
        form.setDayMoment(reading.getDayMoment());
        form.setNote(reading.getNote());
        model.addAttribute("readingEditForm", form);
        model.addAttribute("reading", reading);
        addEditOptions(model, user, readingId, form);
        return "readings/edit";
    }

    @PostMapping("/readings/{readingId}/edit")
    public String updateReading(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long readingId,
            @Valid @ModelAttribute("readingEditForm") ReadingEditForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        User user = currentUserService.requireUser(principal.getUsername());
        OdometerReading reading = readingService.getOwnedReading(readingId, user.getId());
        if (bindingResult.hasErrors()) {
            model.addAttribute("reading", reading);
            addEditOptions(model, user, readingId, form);
            return "readings/edit";
        }
        try {
            readingService.updateOwned(readingId, user.getId(), form);
        } catch (WarningConfirmationRequiredException exception) {
            model.addAttribute("warnings", exception.getWarnings());
            model.addAttribute("reading", reading);
            addEditOptions(model, user, readingId, form);
            return "readings/edit";
        } catch (EntityNotFoundException exception) {
            bindingResult.rejectValue("vehicleId", "notOwned", "Choose one of your vehicles.");
            model.addAttribute("reading", reading);
            addEditOptions(model, user, readingId, form);
            return "readings/edit";
        }
        redirectAttributes.addFlashAttribute("readingUpdated", true);
        return "redirect:/readings";
    }

    @PostMapping("/readings/{readingId}/delete")
    public String deleteReading(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long readingId,
            RedirectAttributes redirectAttributes) {
        User user = currentUserService.requireUser(principal.getUsername());
        String photoPath = readingService.deleteOwned(readingId, user.getId());
        if (photoPath != null) {
            imageStorageService.delete(photoPath);
        }
        redirectAttributes.addFlashAttribute("readingDeleted", true);
        return "redirect:/readings";
    }

    @GetMapping("/readings/photo/{filename:.+}")
    public ResponseEntity<ByteArrayResource> photo(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable String filename,
            HttpSession session) {
        User user = currentUserService.requireUser(principal.getUsername());
        PendingReading pending = (PendingReading) session.getAttribute(PENDING_READING);
        PendingPhoto pendingPhoto = (PendingPhoto) session.getAttribute(PENDING_PHOTO);
        boolean staged = (pending != null && pending.ownerId().equals(user.getId())
                && filename.equals(pending.photoPath()))
                || (pendingPhoto != null && pendingPhoto.ownerId().equals(user.getId())
                && filename.equals(pendingPhoto.photoPath()));
        if (!staged && !readingService.ownsPhoto(filename, user.getId())) {
            throw new EntityNotFoundException("Photo not found");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(new ByteArrayResource(imageStorageService.load(filename)));
    }

    private void addVehicles(Model model, User user) {
        model.addAttribute("vehicles", vehicleService.findVehicles(user.getId()));
    }

    private void addConfirmOptions(Model model, User user, PendingReading pending, ConfirmReadingForm form) {
        addVehicles(model, user);
        if (model.getAttribute("warnings") == null) {
            List<ReadingWarning> warnings = List.of();
            if (form.getVehicleId() != null && form.getReadingKm() != null && form.getTakenAt() != null) {
                try {
                    warnings = readingService.findWarnings(user.getId(), form.getVehicleId(),
                            form.getTakenAt().atZone(DISPLAY_ZONE).toInstant(), form.getReadingKm());
                } catch (EntityNotFoundException exception) {
                    model.addAttribute("vehicleNotOwned", true);
                }
            }
            model.addAttribute("warnings", warnings);
        }
        model.addAttribute("pendingReading", pending);
        model.addAttribute("sourceLabel", sourceLabel(pending.source()));
    }

    private void addEditOptions(Model model, User user, Long readingId, ReadingEditForm form) {
        addVehicles(model, user);
        if (model.getAttribute("warnings") == null) {
            List<ReadingWarning> warnings = List.of();
            if (form.getVehicleId() != null && form.getReadingKm() != null && form.getTakenAt() != null) {
                try {
                    warnings = readingService.findEditWarnings(user.getId(), readingId, form.getVehicleId(),
                            form.getTakenAt().atZone(DISPLAY_ZONE).toInstant(), form.getReadingKm());
                } catch (EntityNotFoundException exception) {
                    model.addAttribute("vehicleNotOwned", true);
                }
            }
            model.addAttribute("warnings", warnings);
        }
    }

    private static ConfirmReadingForm toForm(PendingReading pending) {
        ConfirmReadingForm form = new ConfirmReadingForm();
        form.setVehicleId(pending.vehicleId());
        form.setReadingKm(pending.initialReadingKm());
        form.setTakenAt(pending.takenAt());
        form.setDayMoment(pending.dayMoment());
        form.setNote(pending.note());
        return form;
    }

    private static String sourceLabel(ReadingSource source) {
        return switch (source) {
            case AI -> "AI detected";
            case AI_EDITED -> "AI edited";
            case MANUAL -> "Manual";
        };
    }
}
