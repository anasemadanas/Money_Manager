package com.moneymanager.service;

import com.moneymanager.dto.TransactionDTO;
import com.moneymanager.model.Transaction;
import com.moneymanager.repository.ITransactionRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TransactionServiceTest {

    private Transaction lastSaved;
    private Transaction lastUpdated;
    private long lastDeleted;

    private final ITransactionRepo stub = new ITransactionRepo() {
        @Override
        public Transaction save(Transaction tx) {
            lastSaved = tx;
            tx.setTransactionId(99L);
            return tx;
        }
        @Override
        public List<Transaction> findByUserFiltered(long userId, LocalDate from, LocalDate to, String category) {
            return List.of();
        }
        @Override
        public void update(Transaction tx) { lastUpdated = tx; }
        @Override
        public void delete(long transactionId) { lastDeleted = transactionId; }
        @Override
        public List<String> findDistinctCategories(long userId) { return List.of(); }
    };

    private TransactionService service;
    private final LocalDate today = LocalDate.now();

    @BeforeEach
    void setUp() {
        service = new TransactionService(stub);
        lastSaved = null;
        lastUpdated = null;
        lastDeleted = -1;
    }

    private TransactionDTO validDto() {
        return new TransactionDTO(0, "Coffee", new BigDecimal("4.50"), "Food", "EXPENSE", today);
    }

    @Test
    void add_validExpense_savesAndReturnsDto() {
        TransactionDTO result = service.add(1L, validDto());
        assertNotNull(lastSaved);
        assertEquals("Coffee", result.name());
        assertEquals(new BigDecimal("4.50"), result.amount());
        assertEquals(99L, result.transactionId());
    }

    @Test
    void add_validIncome_accepted() {
        TransactionDTO dto = new TransactionDTO(0, "Salary", new BigDecimal("3000"), "Other", "INCOME", today);
        assertDoesNotThrow(() -> service.add(1L, dto));
        assertEquals("INCOME", lastSaved.getTxType());
    }

    @Test
    void add_nullName_throws() {
        TransactionDTO dto = new TransactionDTO(0, null, new BigDecimal("10"), "Food", "EXPENSE", today);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.add(1L, dto));
        assertTrue(ex.getMessage().contains("Name is required"));
    }

    @Test
    void add_blankName_throws() {
        TransactionDTO dto = new TransactionDTO(0, "   ", new BigDecimal("10"), "Food", "EXPENSE", today);
        assertThrows(IllegalArgumentException.class, () -> service.add(1L, dto));
    }

    @Test
    void add_zeroAmount_throws() {
        TransactionDTO dto = new TransactionDTO(0, "Rent", BigDecimal.ZERO, "Bills", "EXPENSE", today);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.add(1L, dto));
        assertTrue(ex.getMessage().contains("greater than zero"));
    }

    @Test
    void add_negativeAmount_throws() {
        TransactionDTO dto = new TransactionDTO(0, "Rent", new BigDecimal("-50"), "Bills", "EXPENSE", today);
        assertThrows(IllegalArgumentException.class, () -> service.add(1L, dto));
    }

    @Test
    void add_nullAmount_throws() {
        TransactionDTO dto = new TransactionDTO(0, "Rent", null, "Bills", "EXPENSE", today);
        assertThrows(IllegalArgumentException.class, () -> service.add(1L, dto));
    }

    @Test
    void add_blankCategory_throws() {
        TransactionDTO dto = new TransactionDTO(0, "Coffee", new BigDecimal("4"), "", "EXPENSE", today);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.add(1L, dto));
        assertTrue(ex.getMessage().contains("Category is required"));
    }

    @Test
    void add_invalidTxType_throws() {
        TransactionDTO dto = new TransactionDTO(0, "Coffee", new BigDecimal("4"), "Food", "TRANSFER", today);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.add(1L, dto));
        assertTrue(ex.getMessage().contains("INCOME or EXPENSE"));
    }

    @Test
    void add_nullTxType_throws() {
        TransactionDTO dto = new TransactionDTO(0, "Coffee", new BigDecimal("4"), "Food", null, today);
        assertThrows(IllegalArgumentException.class, () -> service.add(1L, dto));
    }

    @Test
    void add_nullDate_throws() {
        TransactionDTO dto = new TransactionDTO(0, "Coffee", new BigDecimal("4"), "Food", "EXPENSE", null);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.add(1L, dto));
        assertTrue(ex.getMessage().contains("Date is required"));
    }

    @Test
    void add_futureDate_throws() {
        LocalDate future = today.plusDays(1);
        TransactionDTO dto = new TransactionDTO(0, "Coffee", new BigDecimal("4"), "Food", "EXPENSE", future);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.add(1L, dto));
        assertTrue(ex.getMessage().contains("future"));
    }

    @Test
    void add_todayDate_accepted() {
        TransactionDTO dto = new TransactionDTO(0, "Coffee", new BigDecimal("4"), "Food", "EXPENSE", today);
        assertDoesNotThrow(() -> service.add(1L, dto));
    }

    @Test
    void update_valid_delegatesToRepo() {
        assertDoesNotThrow(() -> service.update(5L, 1L, validDto()));
        assertNotNull(lastUpdated);
        assertEquals(5L, lastUpdated.getTransactionId());
        assertEquals(1L, lastUpdated.getUserId());
    }

    @Test
    void update_invalidDto_throwsBeforeRepoCall() {
        TransactionDTO bad = new TransactionDTO(5, "", new BigDecimal("10"), "Food", "EXPENSE", today);
        assertThrows(IllegalArgumentException.class, () -> service.update(5L, 1L, bad));
        assertNull(lastUpdated);
    }

    @Test
    void delete_callsRepo() {
        service.delete(42L);
        assertEquals(42L, lastDeleted);
    }
}
