package com.moneymanager.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
public record TransactionDTO(
        long transactionId,
        String name,
        BigDecimal amount,
        String category,
        String txType,
        LocalDate txDate
) {}
