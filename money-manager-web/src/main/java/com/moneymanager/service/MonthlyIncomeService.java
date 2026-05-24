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
    public Optional<BigDecimal> getMonthlyIncome(long userId) {
        return settingsRepo.getMonthlyIncome(userId);
    }
    public void setMonthlyIncome(long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Monthly income must be greater than zero.");
        settingsRepo.setMonthlyIncome(userId, amount);
    }
    public void applyForCurrentMonth(long userId) {
        LocalDate today = LocalDate.now();
        applyForMonth(userId, today.getMonthValue(), today.getYear());
    }
    public void applyForMonth(long userId, int month, int year) {
        Optional<BigDecimal> income = settingsRepo.getMonthlyIncome(userId);
        if (income.isEmpty()) return;

        BigDecimal amount = income.get();
        LocalDate firstOfMonth = LocalDate.of(year, month, 1);
        if (!hasMonthlyIncomeTransaction(userId, month, year)) {
            var tx = new Transaction();
            tx.setUserId(userId);
            tx.setName("Monthly Income");
            tx.setAmount(amount);
            tx.setCategory("Other");
            tx.setTxType("INCOME");
            tx.setTxDate(firstOfMonth);
            txRepo.save(tx);
        }
        if (balanceRepo.findByUserMonthYear(userId, month, year).isEmpty()) {
            balanceRepo.saveOrUpdate(userId, amount, month, year);
        }
    }
    private boolean hasMonthlyIncomeTransaction(long userId, int month, int year) {
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to   = from.withDayOfMonth(from.lengthOfMonth());
        return txRepo.findByUserFiltered(userId, from, to, null).stream()
                .anyMatch(tx -> "INCOME".equals(tx.getTxType())
                             && "Monthly Income".equals(tx.getName()));
    }
}
