package com.fueltrack.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ErrorPageController implements ErrorController {

    @GetMapping("/errors/403")
    public String forbidden() {
        return "error/403";
    }

    @RequestMapping("/error")
    public String error(HttpServletRequest request, HttpServletResponse response, Model model) {
        Object statusAttribute = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int status = statusAttribute instanceof Integer code
                ? code
                : HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        response.setStatus(status);
        if (status == HttpServletResponse.SC_NOT_FOUND) {
            model.addAttribute("message", "The requested page could not be found.");
            return "error/404";
        }
        if (status == HttpServletResponse.SC_FORBIDDEN) {
            return "error/403";
        }
        if (status == HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE) {
            model.addAttribute("message", "The uploaded file is too large. Photos must be 10 MB or smaller.");
            return "error/413";
        }
        if (status == HttpServletResponse.SC_METHOD_NOT_ALLOWED) {
            model.addAttribute("message", "That action is not available for this page.");
            return "error/405";
        }
        return "error/500";
    }
}
