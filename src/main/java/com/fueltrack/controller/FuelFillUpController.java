package com.fueltrack.controller;

import com.fueltrack.dto.FuelFillUpForm;
import com.fueltrack.model.FuelFillUp;
import com.fueltrack.model.User;
import com.fueltrack.service.CurrentUserService;
import com.fueltrack.service.FuelConsumptionCalculator;
import com.fueltrack.service.FuelFillUpService;
import com.fueltrack.service.VehicleService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Controller
public class FuelFillUpController {

    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Africa/Gaborone");

    private final CurrentUserService currentUserService;
    private final VehicleService vehicleService;
    private final FuelFillUpService fillUpService;
    private final FuelConsumptionCalculator consumptionCalculator;
    private final Clock clock;

    public FuelFillUpController(
            CurrentUserService currentUserService,
            VehicleService vehicleService,
            FuelFillUpService fillUpService,
            FuelConsumptionCalculator consumptionCalculator,
            Clock clock) {
        this.currentUserService = currentUserService;
        this.vehicleService = vehicleService;
        this.fillUpService = fillUpService;
        this.consumptionCalculator = consumptionCalculator;
        this.clock = clock;
    }

    @GetMapping("/fill-ups")
    public String fillUps(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = currentUserService.requireUser(principal.getUsername());
        FuelFillUpForm form = new FuelFillUpForm();
        form.setFilledAt(LocalDateTime.now(clock.withZone(DISPLAY_ZONE)));
        model.addAttribute("fillUpForm", form);
        model.addAttribute("editingFillUpId", null);
        addLists(model, user);
        return "fill-ups";
    }

    @GetMapping("/fill-ups/{fillUpId}/edit")
    public String editForm(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long fillUpId,
            Model model) {
        User user = currentUserService.requireUser(principal.getUsername());
        FuelFillUp fillUp = fillUpService.getOwned(fillUpId, user.getId());
        model.addAttribute("fillUpForm", toForm(fillUp));
        model.addAttribute("editingFillUpId", fillUpId);
        model.addAttribute("editingFillUp", fillUp);
        addLists(model, user);
        return "fill-ups";
    }

    @PostMapping("/fill-ups")
    public String create(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @ModelAttribute("fillUpForm") FuelFillUpForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        User user = currentUserService.requireUser(principal.getUsername());
        if (bindingResult.hasErrors()) {
            model.addAttribute("editingFillUpId", null);
            addLists(model, user);
            return "fill-ups";
        }
        fillUpService.create(user.getId(), form);
        redirectAttributes.addFlashAttribute("fillUpSaved", true);
        return "redirect:/fill-ups";
    }

    @PostMapping("/fill-ups/{fillUpId}/edit")
    public String update(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long fillUpId,
            @Valid @ModelAttribute("fillUpForm") FuelFillUpForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        User user = currentUserService.requireUser(principal.getUsername());
        FuelFillUp existing = fillUpService.getOwned(fillUpId, user.getId());
        if (bindingResult.hasErrors()) {
            model.addAttribute("editingFillUpId", fillUpId);
            model.addAttribute("editingFillUp", existing);
            addLists(model, user);
            return "fill-ups";
        }
        try {
            fillUpService.update(fillUpId, user.getId(), form);
        } catch (EntityNotFoundException exception) {
            bindingResult.rejectValue("vehicleId", "notOwned", "Choose one of your vehicles.");
            model.addAttribute("editingFillUpId", fillUpId);
            model.addAttribute("editingFillUp", existing);
            addLists(model, user);
            return "fill-ups";
        }
        redirectAttributes.addFlashAttribute("fillUpUpdated", true);
        return "redirect:/fill-ups";
    }

    @PostMapping("/fill-ups/{fillUpId}/delete")
    public String delete(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long fillUpId,
            RedirectAttributes redirectAttributes) {
        User user = currentUserService.requireUser(principal.getUsername());
        fillUpService.delete(fillUpId, user.getId());
        redirectAttributes.addFlashAttribute("fillUpDeleted", true);
        return "redirect:/fill-ups";
    }

    private void addLists(Model model, User user) {
        List<FuelFillUp> fillUps = fillUpService.findHistory(user.getId());
        model.addAttribute("vehicles", vehicleService.findVehicles(user.getId()));
        model.addAttribute("fillUps", fillUps);
        model.addAttribute("consumptions", fillUps.stream()
                .collect(java.util.stream.Collectors.groupingBy(fillUp -> fillUp.getVehicle().getId()))
                .values().stream()
                .flatMap(vehicleFillUps -> consumptionCalculator.calculate(vehicleFillUps).stream())
                .toList());
    }

    private FuelFillUpForm toForm(FuelFillUp fillUp) {
        FuelFillUpForm form = new FuelFillUpForm();
        form.setVehicleId(fillUp.getVehicle().getId());
        form.setFilledAt(fillUpService.toLocalDateTime(fillUp));
        form.setLitres(fillUp.getLitres());
        form.setTotalCost(fillUp.getTotalCost());
        form.setOdometerKm(fillUp.getOdometerKm());
        form.setFullTank(fillUp.isFullTank());
        return form;
    }
}
