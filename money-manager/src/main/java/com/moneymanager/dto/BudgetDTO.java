package com.moneymanager.dto;

import java.math.BigDecimal;

public record BudgetDTO(
        long budgetId,
        String category,
        BigDecimal amountCap,
        BigDecimal spent,
        int month,
        int year
) {}
