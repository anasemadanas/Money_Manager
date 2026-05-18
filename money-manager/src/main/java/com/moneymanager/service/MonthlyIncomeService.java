package com.moneymanager.service;

import com.moneymanager.model.Transaction;
import com.moneymanager.repository.IMonthlyBalanceRepo;
import com.moneymanager.repository.ITransactionRepo;
import com.moneymanager.repository.IUserSettingsRepo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public class MonthlyIncomeService {

    private final IUserSettingsRepo settingsRepo;
    private final ITransactionRepo  txRepo;
    private final IMonthlyBalanceRepo balanceRepo;

    public MonthlyIncomeService(IUserSettingsRepo settingsRepo,
                                ITransactionRepo  txRepo,
                                IMonthlyBalanceRepo balanceRepo) {
        this.settingsRepo = settingsRepo;
        this.txRepo       = txRepo;
        this.balanceRepo  = balanceRepo;
    }

    /** Return the user's saved monthly income, or empty if never configured. */
    public Optional<BigDecimal> getMonthlyIncome(long userId) {
        return settingsRepo.getMonthlyIncome(userId);
    }

    /** Persist a new monthly income amount (validates > 0). */
    public void setMonthlyIncome(long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Monthly income must be greater than zero.");
        settingsRepo.setMonthlyIncome(userId, amount);
    }

    public void applyForCurrentMonth(long userId) {
        // Disabled: do not automatically add transaction and budget
    }

    public void applyForMonth(long userId, int month, int year) {
        // Disabled: do not automatically add transaction and budget
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Check for a transaction named "Monthly Income" of type INCOME in this month. */
    private boolean hasMonthlyIncomeTransaction(long userId, int month, int year) {
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to   = from.withDayOfMonth(from.lengthOfMonth());
        return txRepo.findByUserFiltered(userId, from, to, null).stream()
                .anyMatch(tx -> "INCOME".equals(tx.getTxType())
                             && "Monthly Income".equals(tx.getName()));
    }
}
