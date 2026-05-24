package com.moneymanager.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
public record DashboardSnapshot(
        LocalDate month,
        BigDecimal monthIncome,
        BigDecimal monthExpenses,
        BigDecimal netBalance,
        BigDecimal goalSavings,
        BigDecimal availableBalance,
        Map<String, BigDecimal> categoryBreakdown,
        List<MonthlyTrend> monthlyTrend
) {}
