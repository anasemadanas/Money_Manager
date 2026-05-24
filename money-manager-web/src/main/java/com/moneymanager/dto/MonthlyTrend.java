package com.moneymanager.dto;

import java.math.BigDecimal;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;
public record MonthlyTrend(int year, int month, BigDecimal income, BigDecimal expenses) {
    public String label() {
        return Month.of(month).getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + year;
    }
}
