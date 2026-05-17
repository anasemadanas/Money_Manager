package com.moneymanager.repository;

import com.moneymanager.dto.BudgetDTO;
import com.moneymanager.model.Budget;

import java.math.BigDecimal;
import java.util.List;

public interface IBudgetRepo {

    /** Persist a new budget and return it with the generated ID. */
    Budget save(Budget budget);

    /** Update only the spending cap for an existing budget. */
    void updateCap(long budgetId, BigDecimal newCap);

    /** Delete a budget by its primary key. */
    void delete(long budgetId);

    /**
     * Return budgets for a given month/year together with their actual EXPENSE spending,
     * computed via a LEFT JOIN on the transactions table.
     */
    List<BudgetDTO> findWithSpending(long userId, int month, int year);

    /** Return true if a budget already exists for this user/category/month/year. */
    boolean existsByCategory(long userId, String category, int month, int year);

    /**
     * Return the sum of EXPENSE transactions for a category/month/year.
     * If excludeTxId > 0, that transaction is excluded (used for edit validation).
     */
    BigDecimal getCategorySpending(long userId, String category, int month, int year, long excludeTxId);

    /**
     * Return the sum of ALL EXPENSE transactions for a month/year.
     * If excludeTxId > 0, that transaction is excluded (used for edit validation).
     */
    BigDecimal getTotalMonthlyExpenses(long userId, int month, int year, long excludeTxId);
}
