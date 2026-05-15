package com.moneymanager.dto;

import java.math.BigDecimal;

/**
 * Carries a budget row plus its actual spending (computed by a JOIN in the repo).
 * budgetId is 0 for unsaved budgets.
 */
public record BudgetDTO(
        long budgetId,
        String category,
        BigDecimal amountCap,
        BigDecimal spent,
        int month,
        int year
) {}
