package com.moneymanager.repository;

import com.moneymanager.model.Contribution;
import com.moneymanager.model.Goal;

import java.math.BigDecimal;
import java.util.List;

public interface IGoalRepo {

    Goal save(Goal goal);

    List<Goal> findByUser(long userId);

    void update(Goal goal);

    void delete(long goalId, long userId);

    void addContribution(long goalId, long userId, BigDecimal amount, String note);

    List<Contribution> getContributions(long goalId, long userId);

    BigDecimal getTotalSavedAmount(long userId);
}
