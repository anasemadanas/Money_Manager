package com.moneymanager.service;

import com.moneymanager.dto.TransactionDTO;
import com.moneymanager.model.Transaction;
import com.moneymanager.repository.ITransactionRepo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class TransactionService {

    private final ITransactionRepo txRepo;

    public TransactionService(ITransactionRepo txRepo) {
        this.txRepo = txRepo;
    }

    public List<TransactionDTO> getFiltered(long userId, LocalDate from, LocalDate to, String category) {
        return txRepo.findByUserFiltered(userId, from, to, category)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public TransactionDTO add(long userId, TransactionDTO dto) {
        validate(dto);
        var tx = new Transaction();
        tx.setUserId(userId);
        tx.setName(dto.name());
        tx.setAmount(dto.amount());
        tx.setCategory(dto.category());
        tx.setTxType(dto.txType());
        tx.setTxDate(dto.txDate());
        return toDto(txRepo.save(tx));
    }

    public void update(long transactionId, long userId, TransactionDTO dto) {
        validate(dto);
        var tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setUserId(userId);
        tx.setName(dto.name());
        tx.setAmount(dto.amount());
        tx.setCategory(dto.category());
        tx.setTxType(dto.txType());
        tx.setTxDate(dto.txDate());
        txRepo.update(tx);
    }

    public void delete(long transactionId) {
        txRepo.delete(transactionId);
    }

    public List<String> getCategories(long userId) {
        return txRepo.findDistinctCategories(userId);
    }

    private void validate(TransactionDTO dto) {
        if (dto.name() == null || dto.name().isBlank()) {
            throw new IllegalArgumentException("Name is required.");
        }
        if (dto.amount() == null || dto.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
        if (dto.category() == null || dto.category().isBlank()) {
            throw new IllegalArgumentException("Category is required.");
        }
        if (dto.txType() == null || (!dto.txType().equals("INCOME") && !dto.txType().equals("EXPENSE"))) {
            throw new IllegalArgumentException("Type must be INCOME or EXPENSE.");
        }
        if (dto.txDate() == null) {
            throw new IllegalArgumentException("Date is required.");
        }
        if (dto.txDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Transaction date cannot be in the future.");
        }
    }

    private TransactionDTO toDto(Transaction tx) {
        return new TransactionDTO(
                tx.getTransactionId(),
                tx.getName(),
                tx.getAmount(),
                tx.getCategory(),
                tx.getTxType(),
                tx.getTxDate()
        );
    }
}
