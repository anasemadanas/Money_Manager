package com.moneymanager.service;

import com.moneymanager.dto.BudgetDTO;
import com.moneymanager.model.Budget;
import com.moneymanager.model.MonthlyBalance;
import com.moneymanager.repository.IBudgetRepo;
import com.moneymanager.repository.IMonthlyBalanceRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class BudgetServiceTest {
    private boolean existsByCategory = false;
    private List<BudgetDTO> budgetDtos = new ArrayList<>();
    private BigDecimal categorySpending = BigDecimal.ZERO;
    private BigDecimal totalExpenses = BigDecimal.ZERO;
    private Budget lastSaved;
    private long lastDeletedBudget = -1;

    private Optional<MonthlyBalance> monthlyBalance = Optional.empty();
    private boolean balanceSaved = false;

    private final IBudgetRepo budgetStub = new IBudgetRepo() {
        @Override
        public Budget save(Budget budget) { lastSaved = budget; budget.setBudgetId(10L); return budget; }
        @Override
        public void updateCap(long budgetId, BigDecimal newCap) {}
        @Override
        public void delete(long budgetId) { lastDeletedBudget = budgetId; }
        @Override
        public List<BudgetDTO> findWithSpending(long userId, int month, int year) { return budgetDtos; }
        @Override
        public boolean existsByCategory(long userId, String category, int month, int year) { return existsByCategory; }
        @Override
        public BigDecimal getCategorySpending(long userId, String category, int month, int year, long excludeTxId) { return categorySpending; }
        @Override
        public BigDecimal getTotalMonthlyExpenses(long userId, int month, int year, long excludeTxId) { return totalExpenses; }
    };

    private final IMonthlyBalanceRepo balanceStub = new IMonthlyBalanceRepo() {
        @Override
        public Optional<MonthlyBalance> findByUserMonthYear(long userId, int month, int year) { return monthlyBalance; }
        @Override
        public void saveOrUpdate(long userId, BigDecimal totalAmount, int month, int year) { balanceSaved = true; }
    };

    private BudgetService service;

    @BeforeEach
    void setUp() {
        service = new BudgetService(budgetStub, balanceStub);
        existsByCategory = false;
        budgetDtos = new ArrayList<>();
        categorySpending = BigDecimal.ZERO;
        totalExpenses = BigDecimal.ZERO;
        lastSaved = null;
        lastDeletedBudget = -1;
        monthlyBalance = Optional.empty();
        balanceSaved = false;
    }

    @Test
    void add_valid_savesbudget() {
        service.add(1L, "Food", new BigDecimal("200"), 5, 2025);
        assertNotNull(lastSaved);
        assertEquals("Food", lastSaved.getCategory());
        assertEquals(new BigDecimal("200"), lastSaved.getAmountCap());
    }

    @Test
    void add_blankCategory_throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.add(1L, "", new BigDecimal("100"), 5, 2025));
        assertTrue(ex.getMessage().contains("Category is required"));
    }

    @Test
    void add_nullCategory_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> service.add(1L, null, new BigDecimal("100"), 5, 2025));
    }

    @Test
    void add_zeroCap_throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.add(1L, "Food", BigDecimal.ZERO, 5, 2025));
        assertTrue(ex.getMessage().contains("greater than zero"));
    }

    @Test
    void add_negativeCap_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> service.add(1L, "Food", new BigDecimal("-10"), 5, 2025));
    }

    @Test
    void add_nullCap_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> service.add(1L, "Food", null, 5, 2025));
    }

    @Test
    void add_invalidMonth_throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.add(1L, "Food", new BigDecimal("100"), 13, 2025));
        assertTrue(ex.getMessage().contains("Invalid month"));
    }

    @Test
    void add_monthZero_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> service.add(1L, "Food", new BigDecimal("100"), 0, 2025));
    }

    @Test
    void add_yearBefore2020_throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.add(1L, "Food", new BigDecimal("100"), 5, 2019));
        assertTrue(ex.getMessage().contains("2020"));
    }

    @Test
    void add_duplicateBudget_throws() {
        existsByCategory = true;
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.add(1L, "Food", new BigDecimal("100"), 5, 2025));
        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    void updateCap_valid_doesNotThrow() {
        assertDoesNotThrow(() -> service.updateCap(1L, new BigDecimal("300")));
    }

    @Test
    void updateCap_zeroCap_throws() {
        assertThrows(IllegalArgumentException.class, () -> service.updateCap(1L, BigDecimal.ZERO));
    }

    @Test
    void delete_callsRepo() {
        service.delete(7L);
        assertEquals(7L, lastDeletedBudget);
    }

    @Test
    void setMonthlyBalance_valid_savesBalance() {
        service.setMonthlyBalance(1L, new BigDecimal("2000"), 5, 2025);
        assertTrue(balanceSaved);
    }

    @Test
    void setMonthlyBalance_zero_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> service.setMonthlyBalance(1L, BigDecimal.ZERO, 5, 2025));
    }

    @Test
    void setMonthlyBalance_null_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> service.setMonthlyBalance(1L, null, 5, 2025));
    }

    @Test
    void checkCategoryLimit_noBudget_returnsNull() {
        budgetDtos = List.of();
        String result = service.checkCategoryLimit(1L, "Food", new BigDecimal("50"),
                LocalDate.of(2025, 5, 10), 0L);
        assertNull(result);
    }

    @Test
    void checkCategoryLimit_underCap_returnsNull() {
        budgetDtos = List.of(new BudgetDTO(1L, "Food", new BigDecimal("200"),
                new BigDecimal("100"), 5, 2025));
        categorySpending = new BigDecimal("100");
        String result = service.checkCategoryLimit(1L, "Food", new BigDecimal("50"),
                LocalDate.of(2025, 5, 10), 0L);
        assertNull(result);
    }

    @Test
    void checkCategoryLimit_exceedsCap_returnsErrorMessage() {
        budgetDtos = List.of(new BudgetDTO(1L, "Food", new BigDecimal("200"),
                new BigDecimal("180"), 5, 2025));
        categorySpending = new BigDecimal("180");
        String result = service.checkCategoryLimit(1L, "Food", new BigDecimal("50"),
                LocalDate.of(2025, 5, 10), 0L);
        assertNotNull(result);
        assertTrue(result.contains("exceed"));
        assertTrue(result.contains("Food"));
    }

    @Test
    void checkCategoryLimit_exactlyCap_returnsNull() {
        budgetDtos = List.of(new BudgetDTO(1L, "Food", new BigDecimal("200"),
                new BigDecimal("150"), 5, 2025));
        categorySpending = new BigDecimal("150");
        String result = service.checkCategoryLimit(1L, "Food", new BigDecimal("50"),
                LocalDate.of(2025, 5, 10), 0L);
        assertNull(result);
    }

    @Test
    void checkMonthlyBalanceLimit_noBalance_returnsNull() {
        monthlyBalance = Optional.empty();
        assertNull(service.checkMonthlyBalanceLimit(1L, new BigDecimal("999"),
                LocalDate.of(2025, 5, 10), 0L));
    }

    @Test
    void checkMonthlyBalanceLimit_withinBalance_returnsNull() {
        MonthlyBalance mb = new MonthlyBalance();
        mb.setTotalAmount(new BigDecimal("1000"));
        monthlyBalance = Optional.of(mb);
        totalExpenses = new BigDecimal("500");
        assertNull(service.checkMonthlyBalanceLimit(1L, new BigDecimal("400"),
                LocalDate.of(2025, 5, 10), 0L));
    }

    @Test
    void checkMonthlyBalanceLimit_exceedsBalance_returnsError() {
        MonthlyBalance mb = new MonthlyBalance();
        mb.setTotalAmount(new BigDecimal("1000"));
        monthlyBalance = Optional.of(mb);
        totalExpenses = new BigDecimal("800");
        String result = service.checkMonthlyBalanceLimit(1L, new BigDecimal("300"),
                LocalDate.of(2025, 5, 10), 0L);
        assertNotNull(result);
        assertTrue(result.contains("monthly budget"));
    }

    @Test
    void getCategoryWarning_below80Percent_returnsNull() {
        budgetDtos = List.of(new BudgetDTO(1L, "Food", new BigDecimal("200"),
                new BigDecimal("100"), 5, 2025));
        assertNull(service.getCategoryWarning(1L, "Food", LocalDate.of(2025, 5, 10)));
    }

    @Test
    void getCategoryWarning_between80And100_returnsAmberMessage() {
        budgetDtos = List.of(new BudgetDTO(1L, "Food", new BigDecimal("200"),
                new BigDecimal("170"), 5, 2025));
        String msg = service.getCategoryWarning(1L, "Food", LocalDate.of(2025, 5, 10));
        assertNotNull(msg);
        assertTrue(msg.contains("85") || msg.contains("Food"));
        assertFalse(msg.contains("100%"));
    }

    @Test
    void getCategoryWarning_at100Percent_returnsRedMessage() {
        budgetDtos = List.of(new BudgetDTO(1L, "Food", new BigDecimal("200"),
                new BigDecimal("200"), 5, 2025));
        String msg = service.getCategoryWarning(1L, "Food", LocalDate.of(2025, 5, 10));
        assertNotNull(msg);
        assertTrue(msg.contains("100%"));
    }

    @Test
    void getCategoryWarning_noBudgetForCategory_returnsNull() {
        budgetDtos = List.of(new BudgetDTO(1L, "Bills", new BigDecimal("200"),
                new BigDecimal("200"), 5, 2025));
        assertNull(service.getCategoryWarning(1L, "Food", LocalDate.of(2025, 5, 10)));
    }
}
