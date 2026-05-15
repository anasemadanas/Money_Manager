package com.moneymanager.model;

import java.math.BigDecimal;

public class MonthlyBalance {

    private long balanceId;
    private long userId;
    private int month;
    private int year;
    private BigDecimal totalAmount;

    public MonthlyBalance() {}

    public long getBalanceId() { return balanceId; }
    public void setBalanceId(long balanceId) { this.balanceId = balanceId; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
}
