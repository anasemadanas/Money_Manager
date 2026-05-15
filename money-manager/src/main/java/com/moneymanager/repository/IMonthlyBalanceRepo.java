package com.moneymanager.repository;

import com.moneymanager.model.MonthlyBalance;

import java.math.BigDecimal;
import java.util.Optional;

public interface IMonthlyBalanceRepo {

    /** Return the monthly balance for this user/month/year, or empty if not set. */
    Optional<MonthlyBalance> findByUserMonthYear(long userId, int month, int year);

    /** Insert or update the monthly budget amount for this user/month/year. */
    void saveOrUpdate(long userId, BigDecimal totalAmount, int month, int year);
}
