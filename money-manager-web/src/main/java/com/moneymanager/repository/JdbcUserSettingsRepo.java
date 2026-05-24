package com.moneymanager.repository;

import com.moneymanager.config.DatabaseConfig;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Optional;

public class JdbcUserSettingsRepo implements IUserSettingsRepo {
    static {
        String ddl = """
                CREATE TABLE IF NOT EXISTS user_settings (
                    user_id        BIGINT        PRIMARY KEY
                                                 REFERENCES users(user_id) ON DELETE CASCADE,
                    monthly_income NUMERIC(12,2)
                )
                """;
        try (var conn = DatabaseConfig.getConnection();
             var ps   = conn.prepareStatement(ddl)) {
            ps.execute();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to create user_settings table", e);
        }
    }

    @Override
    public Optional<BigDecimal> getMonthlyIncome(long userId) {
        var sql = "SELECT monthly_income FROM user_settings WHERE user_id = ?";
        try (var conn = DatabaseConfig.getConnection();
             var ps   = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                BigDecimal val = rs.getBigDecimal("monthly_income");
                return (val != null) ? Optional.of(val) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to read monthly income setting", e);
        }
    }

    @Override
    public void setMonthlyIncome(long userId, BigDecimal amount) {
        var sql = """
                INSERT INTO user_settings (user_id, monthly_income) VALUES (?, ?)
                ON CONFLICT (user_id) DO UPDATE SET monthly_income = EXCLUDED.monthly_income
                """;
        try (var conn = DatabaseConfig.getConnection();
             var ps   = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setBigDecimal(2, amount);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save monthly income setting", e);
        }
    }
}
