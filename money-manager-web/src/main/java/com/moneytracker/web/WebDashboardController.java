package com.moneytracker.web;

import com.moneymanager.service.DashboardService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebDashboardController {

    private final DashboardService dashboardService;

    public WebDashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/")
    public String dashboard(HttpSession session, Model model) {
        Long userId = currentUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }

        model.addAttribute("username", session.getAttribute(WebAuthController.SESSION_USERNAME));
        model.addAttribute("snapshot", dashboardService.getSnapshot(userId));
        return "dashboard";
    }

    static Long currentUserId(HttpSession session) {
        Object value = session.getAttribute(WebAuthController.SESSION_USER_ID);
        return value instanceof Long userId ? userId : null;
    }
}
