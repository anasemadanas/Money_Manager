package com.moneymanager.repository;

import com.moneymanager.dto.BudgetDTO;
import com.moneymanager.model.Budget;

import java.math.BigDecimal;
import java.util.List;

public interface IBudgetRepo {
    Budget save(Budget budget);
    void updateCap(long budgetId, BigDecimal newCap);
    void delete(long budgetId);
    List<BudgetDTO> findWithSpending(long userId, int month, int year);
    boolean existsByCategory(long userId, String category, int month, int year);
    BigDecimal getCategorySpending(long userId, String category, int month, int year, long excludeTxId);
    BigDecimal getTotalMonthlyExpenses(long userId, int month, int year, long excludeTxId);
}
