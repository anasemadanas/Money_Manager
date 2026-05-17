package com.moneymanager.dto;

import java.math.BigDecimal;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

/** Income and expense totals for a single calendar month. */
public record MonthlyTrend(int year, int month, BigDecimal income, BigDecimal expenses) {

    /** Short label for the BarChart X-axis, e.g. "Jan 2025". */
    public String label() {
        return Month.of(month).getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + year;
    }
}
