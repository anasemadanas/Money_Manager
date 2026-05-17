package com.moneymanager.repository;

import com.moneymanager.config.DatabaseConfig;
import com.moneymanager.dto.BudgetDTO;
import com.moneymanager.model.Budget;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class JdbcBudgetRepo implements IBudgetRepo {

    @Override
    public Budget save(Budget budget) {
        var sql = """
                INSERT INTO budgets (user_id, category, amount_cap, month, year)
                VALUES (?, ?, ?, ?, ?) RETURNING budget_id
                """;
        try (var conn = DatabaseConfig.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setLong(1, budget.getUserId());
            ps.setString(2, budget.getCategory());
            ps.setBigDecimal(3, budget.getAmountCap());
            ps.setInt(4, budget.getMonth());
            ps.setInt(5, budget.getYear());
            try (var rs = ps.executeQuery()) {
                if (rs.next()) budget.setBudgetId(rs.getLong("budget_id"));
            }
            return budget;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save budget", e);
        }
    }

    @Override
    public void updateCap(long budgetId, BigDecimal newCap) {
        var sql = "UPDATE budgets SET amount_cap = ? WHERE budget_id = ?";
        try (var conn = DatabaseConfig.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, newCap);
            ps.setLong(2, budgetId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update budget cap", e);
        }
    }

    @Override
    public void delete(long budgetId) {
        try (var conn = DatabaseConfig.getConnection();
             var ps = conn.prepareStatement("DELETE FROM budgets WHERE budget_id = ?")) {
            ps.setLong(1, budgetId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete budget", e);
        }
    }

    /**
     * Single LEFT JOIN query that returns each budget together with the sum of
     * EXPENSE transactions for the same user/category/month/year.
     */
    @Override
    public List<BudgetDTO> findWithSpending(long userId, int month, int year) {
        var sql = """
                SELECT b.budget_id, b.category, b.amount_cap, b.month, b.year,
                       COALESCE(
                           SUM(CASE WHEN t.tx_type = 'EXPENSE' THEN t.amount ELSE 0 END),
                           0
                       ) AS spent
                FROM budgets b
                LEFT JOIN transactions t
                       ON t.user_id = b.user_id
                      AND t.category = b.category
                      AND EXTRACT(MONTH FROM t.tx_date) = b.month
                      AND EXTRACT(YEAR  FROM t.tx_date) = b.year
                WHERE b.user_id = ? AND b.month = ? AND b.year = ?
                GROUP BY b.budget_id, b.category, b.amount_cap, b.month, b.year
                ORDER BY b.category
                """;
        try (var conn = DatabaseConfig.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, month);
            ps.setInt(3, year);
            try (var rs = ps.executeQuery()) {
                var result = new ArrayList<BudgetDTO>();
                while (rs.next()) {
                    result.add(new BudgetDTO(
                            rs.getLong("budget_id"),
                            rs.getString("category"),
                            rs.getBigDecimal("amount_cap"),
                            rs.getBigDecimal("spent"),
                            rs.getInt("month"),
                            rs.getInt("year")
                    ));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query budgets with spending", e);
        }
    }

    @Override
    public boolean existsByCategory(long userId, String category, int month, int year) {
        var sql = "SELECT COUNT(*) FROM budgets WHERE user_id=? AND category=? AND month=? AND year=?";
        try (var conn = DatabaseConfig.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, category);
            ps.setInt(3, month);
            ps.setInt(4, year);
            try (var rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to check budget existence", e);
        }
    }

    @Override
    public BigDecimal getCategorySpending(long userId, String category, int month, int year, long excludeTxId) {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM transactions " +
                     "WHERE user_id=? AND category=? AND tx_type='EXPENSE' " +
                     "AND EXTRACT(MONTH FROM tx_date)=? AND EXTRACT(YEAR FROM tx_date)=?";
        if (excludeTxId > 0) sql += " AND transaction_id != ?";
        try (var conn = DatabaseConfig.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, category);
            ps.setInt(3, month);
            ps.setInt(4, year);
            if (excludeTxId > 0) ps.setLong(5, excludeTxId);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to get category spending", e);
        }
    }

    @Override
    public BigDecimal getTotalMonthlyExpenses(long userId, int month, int year, long excludeTxId) {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM transactions " +
                     "WHERE user_id=? AND tx_type='EXPENSE' " +
                     "AND EXTRACT(MONTH FROM tx_date)=? AND EXTRACT(YEAR FROM tx_date)=?";
        if (excludeTxId > 0) sql += " AND transaction_id != ?";
        try (var conn = DatabaseConfig.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, month);
            ps.setInt(3, year);
            if (excludeTxId > 0) ps.setLong(4, excludeTxId);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to get total monthly expenses", e);
        }
    }
}
