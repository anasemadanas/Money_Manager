package com.moneymanager.repository;

import com.moneymanager.model.Transaction;

import java.time.LocalDate;
import java.util.List;

public interface ITransactionRepo {

    /** Persist a new transaction and return it with the generated ID. */
    Transaction save(Transaction tx);

    /**
     * Return all transactions for a user, with optional filters.
     * Pass null for any filter to skip it.
     */
    List<Transaction> findByUserFiltered(long userId, LocalDate from, LocalDate to, String category);

    /** Update all mutable fields of an existing transaction. */
    void update(Transaction tx);

    /** Delete a transaction by its primary key. */
    void delete(long transactionId);

    /** Return all distinct category strings for a user, sorted alphabetically. */
    List<String> findDistinctCategories(long userId);
}
