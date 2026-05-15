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

    /**
     * Called on login and whenever the setting changes.
     * For the current month:
     *   1. If a "Monthly Income" INCOME transaction does not exist → create one.
     *   2. If no monthly budget cap is set → set it to the monthly income amount.
     * If no income is configured yet, this is a no-op.
     */
    public void applyForCurrentMonth(long userId) {
        LocalDate today = LocalDate.now();
        applyForMonth(userId, today.getMonthValue(), today.getYear());
    }

    /**
     * Same as applyForCurrentMonth but for an arbitrary month — useful for testing
     * and for any future "backfill on month change" logic.
     */
    public void applyForMonth(long userId, int month, int year) {
        Optional<BigDecimal> income = settingsRepo.getMonthlyIncome(userId);
        if (income.isEmpty()) return;

        BigDecimal amount = income.get();
        LocalDate firstOfMonth = LocalDate.of(year, month, 1);

        // ── 1. Auto-create the monthly income transaction if missing ──────────
        if (!hasMonthlyIncomeTransaction(userId, month, year)) {
            var tx = new Transaction();
            tx.setUserId(userId);
            tx.setName("Monthly Income");
            tx.setAmount(amount);
            tx.setCategory("Other");   // generic category for auto-generated income
            tx.setTxType("INCOME");
            tx.setTxDate(firstOfMonth);
            txRepo.save(tx);
        }

        // ── 2. Set monthly budget cap if not already configured ───────────────
        if (balanceRepo.findByUserMonthYear(userId, month, year).isEmpty()) {
            balanceRepo.saveOrUpdate(userId, amount, month, year);
        }
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
