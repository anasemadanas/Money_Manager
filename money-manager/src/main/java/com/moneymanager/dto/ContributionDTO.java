package com.moneymanager.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ContributionDTO(
        long contributionId,
        long goalId,
        BigDecimal amount,
        String note,
        OffsetDateTime contributedAt
) {}
