package com.moneymanager.service;

import com.moneymanager.dto.BudgetDTO;
import com.moneymanager.model.Budget;
import com.moneymanager.model.MonthlyBalance;
import com.moneymanager.repository.IBudgetRepo;
import com.moneymanager.repository.IMonthlyBalanceRepo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class BudgetService {

    private final IBudgetRepo budgetRepo;
    private final IMonthlyBalanceRepo balanceRepo;

    public BudgetService(IBudgetRepo budgetRepo, IMonthlyBalanceRepo balanceRepo) {
        this.budgetRepo  = budgetRepo;
        this.balanceRepo = balanceRepo;
    }

    public List<BudgetDTO> getBudgets(long userId, int month, int year) {
        return budgetRepo.findWithSpending(userId, month, year);
    }

    public void add(long userId, String category, BigDecimal amountCap, int month, int year) {
        validateCategory(category);
        validateCap(amountCap);
        validatePeriod(month, year);
        if (budgetRepo.existsByCategory(userId, category, month, year))
            throw new IllegalArgumentException(
                    "A budget for \"" + category + "\" already exists for this period.");
        var b = new Budget();
        b.setUserId(userId); b.setCategory(category);
        b.setAmountCap(amountCap); b.setMonth(month); b.setYear(year);
        budgetRepo.save(b);
    }

    public void updateCap(long budgetId, BigDecimal newCap) {
        validateCap(newCap);
        budgetRepo.updateCap(budgetId, newCap);
    }

    public void delete(long budgetId) {
        budgetRepo.delete(budgetId);
    }

    public Optional<BigDecimal> getMonthlyBalance(long userId, int month, int year) {
        return balanceRepo.findByUserMonthYear(userId, month, year)
                .map(MonthlyBalance::getTotalAmount);
    }

    public void setMonthlyBalance(long userId, BigDecimal amount, int month, int year) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Monthly budget must be greater than zero.");
        balanceRepo.saveOrUpdate(userId, amount, month, year);
    }

    public BigDecimal getTotalMonthlyExpenses(long userId, int month, int year) {
        return budgetRepo.getTotalMonthlyExpenses(userId, month, year, 0L);
    }

    public String checkCategoryLimit(long userId, String category, BigDecimal amount,
                                      LocalDate txDate, long excludeTxId) {
        int month = txDate.getMonthValue();
        int year  = txDate.getYear();

        return budgetRepo.findWithSpending(userId, month, year).stream()
                .filter(b -> b.category().equals(category))
                .map(budget -> {
                    BigDecimal current = budgetRepo.getCategorySpending(
                            userId, category, month, year, excludeTxId);
                    BigDecimal afterAdd = current.add(amount);
                    if (afterAdd.compareTo(budget.amountCap()) > 0) {
                        BigDecimal left = budget.amountCap().subtract(current)
                                .max(BigDecimal.ZERO)
                                .setScale(2, RoundingMode.HALF_UP);
                        return String.format(
                                "Cannot add this transaction — it would exceed your %s budget.\n" +
                                "Remaining allowance: $%s  (cap: $%s)",
                                category, left,
                                budget.amountCap().setScale(2, RoundingMode.HALF_UP));
                    }
                    return null;
                })
                .filter(msg -> msg != null)
                .findFirst()
                .orElse(null);
    }

    public String checkMonthlyBalanceLimit(long userId, BigDecimal amount,
                                            LocalDate txDate, long excludeTxId) {
        int month = txDate.getMonthValue();
        int year  = txDate.getYear();

        return balanceRepo.findByUserMonthYear(userId, month, year).map(mb -> {
            BigDecimal totalSpent = budgetRepo.getTotalMonthlyExpenses(userId, month, year, excludeTxId);
            BigDecimal afterAdd   = totalSpent.add(amount);
            if (afterAdd.compareTo(mb.getTotalAmount()) > 0) {
                BigDecimal left = mb.getTotalAmount().subtract(totalSpent)
                        .max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
                return String.format(
                        "Cannot add this transaction — it would exceed your monthly budget.\n" +
                        "Remaining balance: $%s  (monthly budget: $%s)",
                        left, mb.getTotalAmount().setScale(2, RoundingMode.HALF_UP));
            }
            return null;
        }).orElse(null);
    }

    public String getCategoryWarning(long userId, String category, LocalDate txDate) {
        int month = txDate.getMonthValue();
        int year  = txDate.getYear();
        String monthName = txDate.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);

        return budgetRepo.findWithSpending(userId, month, year).stream()
                .filter(b -> b.category().equals(category)
                        && b.amountCap().compareTo(BigDecimal.ZERO) > 0)
                .map(b -> {
                    double pct = b.spent()
                            .divide(b.amountCap(), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .doubleValue();
                    if (pct >= 100) {
                        return String.format(
                                "You have reached 100%% of your %s budget for %s %d!",
                                category, monthName, year);
                    } else if (pct >= 80) {
                        return String.format(
                                "You have used %.0f%% of your %s budget for %s %d.",
                                pct, category, monthName, year);
                    }
                    return null;
                })
                .filter(msg -> msg != null)
                .findFirst()
                .orElse(null);
    }

    private static void validateCategory(String c) {
        if (c == null || c.isBlank()) throw new IllegalArgumentException("Category is required.");
    }

    private static void validateCap(BigDecimal cap) {
        if (cap == null || cap.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Budget cap must be greater than zero.");
    }

    private static void validatePeriod(int month, int year) {
        if (month < 1 || month > 12) throw new IllegalArgumentException("Invalid month.");
        if (year < 2020) throw new IllegalArgumentException("Year must be 2020 or later.");
    }
}
