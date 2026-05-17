package com.moneymanager.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Carries transaction data between the service layer and the UI.
 * transactionId is 0 for new (unsaved) transactions.
 */
public record TransactionDTO(
        long transactionId,
        String name,
        BigDecimal amount,
        String category,
        String txType,
        LocalDate txDate
) {}
