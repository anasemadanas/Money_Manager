package com.moneymanager.dto;

import com.moneymanager.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionDTO(
        long transactionId,
        String name,
        BigDecimal amount,
        String category,
        TransactionType txType,
        LocalDate txDate
) {}
