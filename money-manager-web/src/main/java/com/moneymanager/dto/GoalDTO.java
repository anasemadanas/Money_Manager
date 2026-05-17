package com.moneymanager.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * View-facing snapshot of a savings goal.
 * savedAmount is the running total from goal_contributions.
 * deadline is null when no deadline was set.
 */
public record GoalDTO(
        long goalId,
        String name,
        BigDecimal targetAmount,
        BigDecimal savedAmount,
        LocalDate deadline
) {}
