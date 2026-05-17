package com.moneymanager.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Carries one row from goal_contributions for display in the history table. */
public record ContributionDTO(
        long contributionId,
        long goalId,
        BigDecimal amount,
        String note,                // null / blank = no memo
        OffsetDateTime contributedAt
) {}
