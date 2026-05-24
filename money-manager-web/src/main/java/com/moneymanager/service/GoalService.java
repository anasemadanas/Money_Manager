package com.moneymanager.service;

import com.moneymanager.dto.ContributionDTO;
import com.moneymanager.dto.GoalDTO;
import com.moneymanager.model.Goal;
import com.moneymanager.repository.IGoalRepo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class GoalService {

    private final IGoalRepo goalRepo;

    public GoalService(IGoalRepo goalRepo) {
        this.goalRepo = goalRepo;
    }
    public List<GoalDTO> getGoals(long userId) {
        return goalRepo.findByUser(userId).stream().map(this::toDto).toList();
    }
    public void addGoal(long userId, String name, BigDecimal targetAmount, LocalDate deadline) {
        validateName(name);
        validateTarget(targetAmount);
        var goal = new Goal();
        goal.setUserId(userId);
        goal.setName(name.trim());
        goal.setTargetAmount(targetAmount);
        goal.setDeadline(deadline);
        goalRepo.save(goal);
    }
    public void updateGoal(long goalId, String name, BigDecimal targetAmount, LocalDate deadline) {
        validateName(name);
        validateTarget(targetAmount);
        var goal = new Goal();
        goal.setGoalId(goalId);
        goal.setName(name.trim());
        goal.setTargetAmount(targetAmount);
        goal.setDeadline(deadline);
        goalRepo.update(goal);
    }
    public void deleteGoal(long goalId) {
        goalRepo.delete(goalId);
    }
    public void addContribution(long goalId, BigDecimal amount, String note) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Contribution amount must be greater than zero.");
        goalRepo.addContribution(goalId, amount, note);
    }
    public List<ContributionDTO> getContributions(long goalId) {
        return goalRepo.getContributions(goalId).stream()
                .map(c -> new ContributionDTO(
                        c.getContributionId(), c.getGoalId(),
                        c.getAmount(), c.getNote(), c.getContributedAt()))
                .toList();
    }

    private GoalDTO toDto(Goal g) {
        return new GoalDTO(g.getGoalId(), g.getName(),
                g.getTargetAmount(), g.getSavedAmount(), g.getDeadline());
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Goal name is required.");
    }

    private static void validateTarget(BigDecimal target) {
        if (target == null || target.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Target amount must be greater than zero.");
    }
}
