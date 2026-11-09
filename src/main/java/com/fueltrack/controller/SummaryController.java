package com.fueltrack.controller;

import com.fueltrack.model.User;
import com.fueltrack.service.CurrentUserService;
import com.fueltrack.service.MileageSummaryService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SummaryController {

    private final CurrentUserService currentUserService;
    private final MileageSummaryService summaryService;

    public SummaryController(CurrentUserService currentUserService, MileageSummaryService summaryService) {
        this.currentUserService = currentUserService;
        this.summaryService = summaryService;
    }

    @GetMapping("/summary")
    public String summary(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = currentUserService.requireUser(principal.getUsername());
        MileageSummaryService.MileageSummary summary = summaryService.summarize(user.getId());
        model.addAttribute("dailySummaries", summary.daily());
        model.addAttribute("monthlySummaries", summary.monthly());
        return "summary";
    }
}
