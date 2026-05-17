package com.moneymanager.model;

import java.math.BigDecimal;

public class Budget {

    private long budgetId;
    private long userId;
    private String category;
    private BigDecimal amountCap;
    private int month;
    private int year;

    public Budget() {}

    public long getBudgetId() { return budgetId; }
    public void setBudgetId(long budgetId) { this.budgetId = budgetId; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public BigDecimal getAmountCap() { return amountCap; }
    public void setAmountCap(BigDecimal amountCap) { this.amountCap = amountCap; }

    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
}
