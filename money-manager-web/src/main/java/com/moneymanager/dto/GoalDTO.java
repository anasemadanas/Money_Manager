package com.moneymanager.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
public record GoalDTO(
        long goalId,
        String name,
        BigDecimal targetAmount,
        BigDecimal savedAmount,
        LocalDate deadline
) {}
