package com.fueltrack.controller;

import com.fueltrack.dto.VehicleForm;
import com.fueltrack.model.User;
import com.fueltrack.service.CurrentUserService;
import com.fueltrack.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class VehicleController {

    private final CurrentUserService currentUserService;
    private final VehicleService vehicleService;

    public VehicleController(CurrentUserService currentUserService, VehicleService vehicleService) {
        this.currentUserService = currentUserService;
        this.vehicleService = vehicleService;
    }

    @GetMapping("/vehicles")
    public String vehicles(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = currentUserService.requireUser(principal.getUsername());
        model.addAttribute("vehicles", vehicleService.findVehicles(user.getId()));
        model.addAttribute("vehicleForm", new VehicleForm());
        return "vehicles";
    }

    @PostMapping("/vehicles")
    public String createVehicle(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @ModelAttribute("vehicleForm") VehicleForm form,
            BindingResult bindingResult,
            Model model) {
        User user = currentUserService.requireUser(principal.getUsername());
        if (bindingResult.hasErrors()) {
            model.addAttribute("vehicles", vehicleService.findVehicles(user.getId()));
            return "vehicles";
        }
        vehicleService.createVehicle(user, form);
        return "redirect:/vehicles?created";
    }
}
