package com.fueltrack.controller;

import com.fueltrack.dto.RegistrationForm;
import com.fueltrack.service.RegistrationService;
import com.fueltrack.service.RegistrationService.DuplicateEmailException;
import com.fueltrack.service.RegistrationService.PasswordMismatchException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("registrationForm", new RegistrationForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("registrationForm") RegistrationForm form,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "register";
        }

        try {
            registrationService.register(form);
        } catch (DuplicateEmailException ex) {
            bindingResult.rejectValue("email", "duplicate", "An account with this email already exists.");
            return "register";
        } catch (PasswordMismatchException ex) {
            bindingResult.rejectValue("confirmPassword", "mismatch", "Passwords do not match.");
            return "register";
        }
        return "redirect:/login?registered";
    }
}
