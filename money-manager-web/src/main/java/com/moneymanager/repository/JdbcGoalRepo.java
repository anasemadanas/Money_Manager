package com.moneymanager.repository;

import com.moneymanager.config.DatabaseConfig;
import com.moneymanager.model.Contribution;
import com.moneymanager.model.Goal;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class JdbcGoalRepo implements IGoalRepo {

    @Override
    public Goal save(Goal goal) {
        var sql = """
                INSERT INTO savings_goals (user_id, name, target_amount, deadline)
                VALUES (?, ?, ?, ?) RETURNING goal_id, saved_amount, created_at
                """;
        try (var conn = DatabaseConfig.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setLong(1, goal.getUserId());
            ps.setString(2, goal.getName());
            ps.setBigDecimal(3, goal.getTargetAmount());
            if (goal.getDeadline() != null)
                ps.setDate(4, Date.valueOf(goal.getDeadline()));
            else
                ps.setNull(4, Types.DATE);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    goal.setGoalId(rs.getLong("goal_id"));
                    goal.setSavedAmount(rs.getBigDecimal("saved_amount"));
                    goal.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
                }
            }
            return goal;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save goal", e);
        }
    }

    @Override
    public List<Goal> findByUser(long userId) {
        var sql = """
                SELECT goal_id, user_id, name, target_amount, saved_amount, deadline, created_at
                FROM savings_goals WHERE user_id = ? ORDER BY created_at DESC
                """;
        try (var conn = DatabaseConfig.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (var rs = ps.executeQuery()) {
                var list = new ArrayList<Goal>();
                while (rs.next()) list.add(mapGoal(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch goals", e);
        }
    }

    @Override
    public void update(Goal goal) {
        var sql = """
                UPDATE savings_goals SET name=?, target_amount=?, deadline=?
                WHERE goal_id=?
                """;
        try (var conn = DatabaseConfig.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, goal.getName());
            ps.setBigDecimal(2, goal.getTargetAmount());
            if (goal.getDeadline() != null)
                ps.setDate(3, Date.valueOf(goal.getDeadline()));
            else
                ps.setNull(3, Types.DATE);
            ps.setLong(4, goal.getGoalId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update goal", e);
        }
    }

    @Override
    public void delete(long goalId) {
        try (var conn = DatabaseConfig.getConnection();
             var ps = conn.prepareStatement(
                     "DELETE FROM savings_goals WHERE goal_id = ?")) {
            ps.setLong(1, goalId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete goal", e);
        }
    }
    @Override
    public void addContribution(long goalId, BigDecimal amount, String note) {
        var insertSql = "INSERT INTO goal_contributions (goal_id, amount, note) VALUES (?, ?, ?)";
        var updateSql = "UPDATE savings_goals SET saved_amount = saved_amount + ? WHERE goal_id = ?";
        try (var conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (var ps = conn.prepareStatement(insertSql)) {
                    ps.setLong(1, goalId);
                    ps.setBigDecimal(2, amount);
                    ps.setString(3, (note != null && !note.isBlank()) ? note : null);
                    ps.executeUpdate();
                }
                try (var ps = conn.prepareStatement(updateSql)) {
                    ps.setBigDecimal(1, amount);
                    ps.setLong(2, goalId);
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to add contribution", e);
        }
    }

    @Override
    public List<Contribution> getContributions(long goalId) {
        var sql = """
                SELECT contribution_id, goal_id, amount, note, contributed_at
                FROM goal_contributions WHERE goal_id = ? ORDER BY contributed_at DESC
                """;
        try (var conn = DatabaseConfig.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setLong(1, goalId);
            try (var rs = ps.executeQuery()) {
                var list = new ArrayList<Contribution>();
                while (rs.next()) {
                    var c = new Contribution();
                    c.setContributionId(rs.getLong("contribution_id"));
                    c.setGoalId(rs.getLong("goal_id"));
                    c.setAmount(rs.getBigDecimal("amount"));
                    c.setNote(rs.getString("note"));
                    c.setContributedAt(rs.getObject("contributed_at", OffsetDateTime.class));
                    list.add(c);
                }
                return list;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch contributions", e);
        }
    }

    @Override
    public BigDecimal getTotalSavedAmount(long userId) {
        var sql = "SELECT COALESCE(SUM(saved_amount), 0) FROM savings_goals WHERE user_id = ?";
        try (var conn = DatabaseConfig.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to get total saved amount", e);
        }
    }

    private Goal mapGoal(ResultSet rs) throws SQLException {
        var g = new Goal();
        g.setGoalId(rs.getLong("goal_id"));
        g.setUserId(rs.getLong("user_id"));
        g.setName(rs.getString("name"));
        g.setTargetAmount(rs.getBigDecimal("target_amount"));
        g.setSavedAmount(rs.getBigDecimal("saved_amount"));
        Date d = rs.getDate("deadline");
        g.setDeadline(d != null ? d.toLocalDate() : null);
        g.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        return g;
    }
}
