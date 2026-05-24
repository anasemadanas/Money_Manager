package com.moneymanager.repository;

import com.moneymanager.config.DatabaseConfig;
import com.moneymanager.model.MonthlyBalance;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Optional;

public class JdbcMonthlyBalanceRepo implements IMonthlyBalanceRepo {
    @Override
    public Optional<MonthlyBalance> findByUserMonthYear(long userId, int month, int year) {
        var sql = "SELECT balance_id, user_id, month, year, total_amount " +
                  "FROM monthly_balance WHERE user_id=? AND month=? AND year=?";
        try (var conn = DatabaseConfig.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, month);
            ps.setInt(3, year);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                var mb = new MonthlyBalance();
                mb.setBalanceId(rs.getLong("balance_id"));
                mb.setUserId(rs.getLong("user_id"));
                mb.setMonth(rs.getInt("month"));
                mb.setYear(rs.getInt("year"));
                mb.setTotalAmount(rs.getBigDecimal("total_amount"));
                return Optional.of(mb);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch monthly balance", e);
        }
    }

    @Override
    public void saveOrUpdate(long userId, BigDecimal totalAmount, int month, int year) {
        var sql = """
                INSERT INTO monthly_balance (user_id, month, year, total_amount)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (user_id, month, year)
                DO UPDATE SET total_amount = EXCLUDED.total_amount
                """;
        try (var conn = DatabaseConfig.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, month);
            ps.setInt(3, year);
            ps.setBigDecimal(4, totalAmount);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save monthly balance", e);
        }
    }
}
