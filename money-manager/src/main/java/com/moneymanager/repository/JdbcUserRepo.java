package com.moneymanager.repository;

import com.moneymanager.config.DatabaseConfig;
import com.moneymanager.model.User;

import java.sql.*;
import java.time.OffsetDateTime;
import java.util.Optional;

public class JdbcUserRepo implements IUserRepo {

    @Override
    public User save(User user) {
        var sql = "INSERT INTO users (username, password_hash) VALUES (?, ?) RETURNING user_id, created_at";
        try (var conn = DatabaseConfig.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    user.setUserId(rs.getLong("user_id"));
                    user.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
                }
            }
            return user;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save user", e);
        }
    }

    @Override
    public Optional<User> findByUsername(String username) {
        var sql = "SELECT user_id, username, password_hash, created_at FROM users WHERE username = ?";
        try (var conn = DatabaseConfig.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find user by username", e);
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        return new User(
                rs.getLong("user_id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getObject("created_at", OffsetDateTime.class)
        );
    }
}
