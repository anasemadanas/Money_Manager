package com.moneymanager.repository;

import com.moneymanager.model.MonthlyBalance;

import java.math.BigDecimal;
import java.util.Optional;

public interface IMonthlyBalanceRepo {
    Optional<MonthlyBalance> findByUserMonthYear(long userId, int month, int year);
    void saveOrUpdate(long userId, BigDecimal totalAmount, int month, int year);
}
