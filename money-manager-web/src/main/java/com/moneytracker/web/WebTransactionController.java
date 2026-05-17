package com.moneytracker.web;

import com.moneymanager.dto.TransactionDTO;
import com.moneymanager.service.BudgetService;
import com.moneymanager.service.TransactionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
public class WebTransactionController {

    private static final List<String> DEFAULT_CATEGORIES = List.of(
            "Food", "Bills", "Groceries", "Entertainment", "Travel", "Other"
    );

    private final TransactionService transactionService;
    private final BudgetService budgetService;

    public WebTransactionController(TransactionService transactionService, BudgetService budgetService) {
        this.transactionService = transactionService;
        this.budgetService = budgetService;
    }

    @GetMapping("/transactions")
    public String transactions(HttpSession session, Model model) {
        Long userId = WebDashboardController.currentUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }

        model.addAttribute("username", session.getAttribute(WebAuthController.SESSION_USERNAME));
        model.addAttribute("transactions", transactionService.getFiltered(userId, null, null, null));
        model.addAttribute("categories", DEFAULT_CATEGORIES);
        model.addAttribute("today", LocalDate.now());
        return "transactions";
    }

    @PostMapping("/transactions")
    public String addTransaction(@RequestParam String name,
                                 @RequestParam BigDecimal amount,
                                 @RequestParam String category,
                                 @RequestParam String txType,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate txDate,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        Long userId = WebDashboardController.currentUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }

        try {
            if ("EXPENSE".equals(txType)) {
                String categoryLimit = budgetService.checkCategoryLimit(userId, category, amount, txDate, 0L);
                if (categoryLimit != null) {
                    throw new IllegalArgumentException(categoryLimit);
                }
                String monthlyLimit = budgetService.checkMonthlyBalanceLimit(userId, amount, txDate, 0L);
                if (monthlyLimit != null) {
                    throw new IllegalArgumentException(monthlyLimit);
                }
            }

            transactionService.add(userId, new TransactionDTO(0, name, amount, category, txType, txDate));
            redirectAttributes.addFlashAttribute("message", "Transaction added.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }

        return "redirect:/transactions";
    }

    @PostMapping("/transactions/delete")
    public String deleteTransaction(@RequestParam long transactionId, HttpSession session) {
        Long userId = WebDashboardController.currentUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }

        transactionService.delete(transactionId);
        return "redirect:/transactions";
    }
}
