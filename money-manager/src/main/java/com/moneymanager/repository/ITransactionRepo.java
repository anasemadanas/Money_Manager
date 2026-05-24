package com.moneymanager.repository;

import com.moneymanager.model.Transaction;

import java.time.LocalDate;
import java.util.List;

public interface ITransactionRepo {

    Transaction save(Transaction tx);

    List<Transaction> findByUserFiltered(long userId, LocalDate from, LocalDate to, String category);

    void update(Transaction tx);

    void delete(long transactionId);

    List<String> findDistinctCategories(long userId);
}
