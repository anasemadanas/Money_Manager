package com.moneymanager.repository;

import com.moneymanager.config.DatabaseConfig;
import com.moneymanager.model.Transaction;
import com.moneymanager.model.TransactionType;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class JdbcTransactionRepo implements ITransactionRepo {

    @Override
    public Transaction save(Transaction tx) {
        var sql = """
                INSERT INTO transactions (user_id, name, amount, category, tx_type, tx_date)
                VALUES (?, ?, ?, ?, ?, ?) RETURNING transaction_id, created_at
                """;
        try (var conn = DatabaseConfig.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setLong(1, tx.getUserId());
            ps.setString(2, tx.getName());
            ps.setBigDecimal(3, tx.getAmount());
            ps.setString(4, tx.getCategory());
            ps.setString(5, tx.getTxType().name());
            ps.setString(6, tx.getTxDate().toString());
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    tx.setTransactionId(rs.getLong("transaction_id"));
                    tx.setCreatedAt(JdbcDates.getOffsetDateTime(rs, "created_at"));
                }
            }
            return tx;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save transaction", e);
        }
    }

    @Override
    public List<Transaction> findByUserFiltered(long userId, LocalDate from, LocalDate to, String category) {
        List<Object> params = new ArrayList<>();
        var sql = new StringBuilder(
                "SELECT transaction_id, user_id, name, amount, category, tx_type, tx_date, created_at " +
                "FROM transactions WHERE user_id = ?");
        params.add(userId);

        if (from != null) {
            sql.append(" AND tx_date >= ?");
            params.add(from.toString());
        }
        if (to != null) {
            sql.append(" AND tx_date <= ?");
            params.add(to.toString());
        }
        if (category != null && !category.isBlank()) {
            sql.append(" AND category = ?");
            params.add(category);
        }
        sql.append(" ORDER BY tx_date DESC, transaction_id DESC");

        try (var conn = DatabaseConfig.getConnection();
             var ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (var rs = ps.executeQuery()) {
                var result = new ArrayList<Transaction>();
                while (rs.next()) result.add(mapRow(rs));
                return result;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query transactions", e);
        }
    }

    @Override
    public void update(Transaction tx) {
        var sql = """
                UPDATE transactions
                SET name = ?, amount = ?, category = ?, tx_type = ?, tx_date = ?
                WHERE transaction_id = ? AND user_id = ?
                """;
        try (var conn = DatabaseConfig.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, tx.getName());
            ps.setBigDecimal(2, tx.getAmount());
            ps.setString(3, tx.getCategory());
            ps.setString(4, tx.getTxType().name());
            ps.setString(5, tx.getTxDate().toString());
            ps.setLong(6, tx.getTransactionId());
            ps.setLong(7, tx.getUserId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update transaction", e);
        }
    }

    @Override
    public void delete(long transactionId, long userId) {
        try (var conn = DatabaseConfig.getConnection();
             var ps = conn.prepareStatement(
                     "DELETE FROM transactions WHERE transaction_id = ? AND user_id = ?")) {
            ps.setLong(1, transactionId);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete transaction", e);
        }
    }

    @Override
    public List<String> findDistinctCategories(long userId) {
        var sql = "SELECT DISTINCT category FROM transactions WHERE user_id = ? ORDER BY category";
        try (var conn = DatabaseConfig.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (var rs = ps.executeQuery()) {
                var result = new ArrayList<String>();
                while (rs.next()) result.add(rs.getString("category"));
                return result;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch categories", e);
        }
    }

    private Transaction mapRow(ResultSet rs) throws SQLException {
        var tx = new Transaction();
        tx.setTransactionId(rs.getLong("transaction_id"));
        tx.setUserId(rs.getLong("user_id"));
        tx.setName(rs.getString("name"));
        tx.setAmount(rs.getBigDecimal("amount"));
        tx.setCategory(rs.getString("category"));
        tx.setTxType(TransactionType.valueOf(rs.getString("tx_type")));
        tx.setTxDate(LocalDate.parse(rs.getString("tx_date")));
        tx.setCreatedAt(JdbcDates.getOffsetDateTime(rs, "created_at"));
        return tx;
    }
}
