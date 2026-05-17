package com.moneymanager.repository;

import com.moneymanager.model.Contribution;
import com.moneymanager.model.Goal;

import java.math.BigDecimal;
import java.util.List;

public interface IGoalRepo {

    /** Persist a new goal and return it with the generated ID and defaults filled. */
    Goal save(Goal goal);

    /** Return all goals for a user, newest first. */
    List<Goal> findByUser(long userId);

    /** Update name, target_amount, and deadline of an existing goal. */
    void update(Goal goal);

    /** Delete a goal (cascades to goal_contributions). */
    void delete(long goalId);

    /**
     * Add a contribution in a single JDBC transaction:
     * insert into goal_contributions AND increment savings_goals.saved_amount.
     */
    void addContribution(long goalId, BigDecimal amount, String note);

    /** Return all contributions for a goal, newest first. */
    List<Contribution> getContributions(long goalId);

    /** Return the all-time sum of saved_amount across every goal for a user. */
    BigDecimal getTotalSavedAmount(long userId);
}
