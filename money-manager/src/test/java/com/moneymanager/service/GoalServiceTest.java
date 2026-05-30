package com.moneymanager.service;

import com.moneymanager.dto.GoalDTO;
import com.moneymanager.model.Contribution;
import com.moneymanager.model.Goal;
import com.moneymanager.repository.IGoalRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GoalServiceTest {

    private List<Goal> storedGoals = new ArrayList<>();
    private Goal lastSaved;
    private Goal lastUpdated;
    private long lastDeletedGoal = -1;
    private long lastContributionGoalId = -1;
    private BigDecimal lastContributionAmount;
    private String lastContributionNote;

    private final IGoalRepo stub = new IGoalRepo() {
        @Override
        public Goal save(Goal goal) {
            lastSaved = goal;
            goal.setGoalId(77L);
            goal.setSavedAmount(BigDecimal.ZERO);
            storedGoals.add(goal);
            return goal;
        }
        @Override
        public List<Goal> findByUser(long userId) { return storedGoals; }
        @Override
        public void update(Goal goal) { lastUpdated = goal; }
        @Override
        public void delete(long goalId, long userId) { lastDeletedGoal = goalId; }
        @Override
        public void addContribution(long goalId, long userId, BigDecimal amount, String note) {
            lastContributionGoalId = goalId;
            lastContributionAmount = amount;
            lastContributionNote = note;
        }
        @Override
        public List<Contribution> getContributions(long goalId, long userId) { return List.of(); }
        @Override
        public BigDecimal getTotalSavedAmount(long userId) { return BigDecimal.ZERO; }
    };

    private GoalService service;

    @BeforeEach
    void setUp() {
        service = new GoalService(stub);
        storedGoals = new ArrayList<>();
        lastSaved = null;
        lastUpdated = null;
        lastDeletedGoal = -1;
        lastContributionGoalId = -1;
        lastContributionAmount = null;
        lastContributionNote = null;
    }

    @Test
    void addGoal_valid_savesGoal() {
        service.addGoal(1L, "New Laptop", new BigDecimal("1000"), null);
        assertNotNull(lastSaved);
        assertEquals("New Laptop", lastSaved.getName());
        assertEquals(new BigDecimal("1000"), lastSaved.getTargetAmount());
        assertEquals(1L, lastSaved.getUserId());
    }

    @Test
    void addGoal_withDeadline_deadlineStored() {
        LocalDate deadline = LocalDate.now().plusDays(90);
        service.addGoal(1L, "Vacation", new BigDecimal("2000"), deadline);
        assertEquals(deadline, lastSaved.getDeadline());
    }

    @Test
    void addGoal_whitespaceInName_trimmed() {
        service.addGoal(1L, "  Car Fund  ", new BigDecimal("5000"), null);
        assertEquals("Car Fund", lastSaved.getName());
    }

    @Test
    void addGoal_nullName_throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.addGoal(1L, null, new BigDecimal("1000"), null));
        assertTrue(ex.getMessage().contains("Goal name is required"));
    }

    @Test
    void addGoal_blankName_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> service.addGoal(1L, "   ", new BigDecimal("1000"), null));
    }

    @Test
    void addGoal_zeroTarget_throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.addGoal(1L, "Laptop", BigDecimal.ZERO, null));
        assertTrue(ex.getMessage().contains("Target amount"));
    }

    @Test
    void addGoal_negativeTarget_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> service.addGoal(1L, "Laptop", new BigDecimal("-100"), null));
    }

    @Test
    void addGoal_nullTarget_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> service.addGoal(1L, "Laptop", null, null));
    }

    @Test
    void updateGoal_valid_callsRepo() {
        service.updateGoal(5L, 1L, "Updated Goal", new BigDecimal("1500"), null);
        assertNotNull(lastUpdated);
        assertEquals(5L, lastUpdated.getGoalId());
        assertEquals(1L, lastUpdated.getUserId());
        assertEquals("Updated Goal", lastUpdated.getName());
    }

    @Test
    void updateGoal_invalidName_throwsBeforeRepo() {
        assertThrows(IllegalArgumentException.class,
                () -> service.updateGoal(5L, 1L, "", new BigDecimal("1500"), null));
        assertNull(lastUpdated);
    }

    @Test
    void updateGoal_invalidTarget_throwsBeforeRepo() {
        assertThrows(IllegalArgumentException.class,
                () -> service.updateGoal(5L, 1L, "Goal", BigDecimal.ZERO, null));
        assertNull(lastUpdated);
    }

    @Test
    void deleteGoal_callsRepo() {
        service.deleteGoal(12L, 1L);
        assertEquals(12L, lastDeletedGoal);
    }

    @Test
    void addContribution_valid_callsRepo() {
        service.addContribution(7L, 1L, new BigDecimal("250"), "Birthday money");
        assertEquals(7L, lastContributionGoalId);
        assertEquals(new BigDecimal("250"), lastContributionAmount);
        assertEquals("Birthday money", lastContributionNote);
    }

    @Test
    void addContribution_nullNote_accepted() {
        assertDoesNotThrow(() -> service.addContribution(7L, 1L, new BigDecimal("100"), null));
    }

    @Test
    void addContribution_zeroAmount_throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.addContribution(7L, 1L, BigDecimal.ZERO, null));
        assertTrue(ex.getMessage().contains("greater than zero"));
    }

    @Test
    void addContribution_negativeAmount_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> service.addContribution(7L, 1L, new BigDecimal("-50"), null));
    }

    @Test
    void addContribution_nullAmount_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> service.addContribution(7L, 1L, null, null));
    }

    @Test
    void getGoals_returnsAllGoalDtos() {
        Goal g1 = buildGoal(1L, "Laptop", new BigDecimal("1000"), new BigDecimal("720"), null);
        Goal g2 = buildGoal(2L, "Car", new BigDecimal("5000"), new BigDecimal("1000"), null);
        storedGoals = List.of(g1, g2);

        List<GoalDTO> dtos = service.getGoals(1L);
        assertEquals(2, dtos.size());
    }

    @Test
    void getGoals_72Percent_progressCorrect() {
        Goal g = buildGoal(1L, "Laptop", new BigDecimal("1000"), new BigDecimal("720"), null);
        storedGoals = List.of(g);

        GoalDTO dto = service.getGoals(1L).get(0);
        assertEquals(new BigDecimal("1000"), dto.targetAmount());
        assertEquals(new BigDecimal("720"), dto.savedAmount());
        double pct = dto.savedAmount().doubleValue() / dto.targetAmount().doubleValue() * 100;
        assertEquals(72.0, pct, 0.001);
    }

    @Test
    void getGoals_goalCompleted_100Percent() {
        Goal g = buildGoal(1L, "Fund", new BigDecimal("500"), new BigDecimal("500"), null);
        storedGoals = List.of(g);

        GoalDTO dto = service.getGoals(1L).get(0);
        double pct = dto.savedAmount().doubleValue() / dto.targetAmount().doubleValue() * 100;
        assertEquals(100.0, pct, 0.001);
    }

    @Test
    void getGoals_deadlinePropagated() {
        LocalDate dl = LocalDate.of(2025, 12, 31);
        Goal g = buildGoal(1L, "Trip", new BigDecimal("2000"), new BigDecimal("100"), dl);
        storedGoals = List.of(g);

        GoalDTO dto = service.getGoals(1L).get(0);
        assertEquals(dl, dto.deadline());
    }

    @Test
    void getGoals_noGoals_returnsEmptyList() {
        storedGoals = List.of();
        assertTrue(service.getGoals(1L).isEmpty());
    }

    @Test
    void getContributions_returnsEmptyListWhenNone() {
        assertTrue(service.getContributions(1L, 1L).isEmpty());
    }

    private Goal buildGoal(long id, String name, BigDecimal target, BigDecimal saved, LocalDate deadline) {
        Goal g = new Goal();
        g.setGoalId(id);
        g.setUserId(1L);
        g.setName(name);
        g.setTargetAmount(target);
        g.setSavedAmount(saved);
        g.setDeadline(deadline);
        return g;
    }
}
