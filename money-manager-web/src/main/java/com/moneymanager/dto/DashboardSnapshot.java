package com.moneymanager.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Everything the DashboardController needs, produced by a single call to
 * DashboardService.getSnapshot(). Avoids multiple round-trips to the UI.
 */
public record DashboardSnapshot(
        LocalDate month,               // the month these KPIs cover
        BigDecimal monthIncome,
        BigDecimal monthExpenses,
        BigDecimal netBalance,         // income − expenses
        BigDecimal goalSavings,        // all-time total across all goals
        BigDecimal availableBalance,   // netBalance − goalSavings
        Map<String, BigDecimal> categoryBreakdown,  // expense totals by category
        List<MonthlyTrend> monthlyTrend             // last 6 months
) {}
