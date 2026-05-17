package com.moneymanager.repository;

import com.moneymanager.config.DatabaseConfig;
import com.moneymanager.model.Note;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class JdbcNoteRepo implements INoteRepo {

    @Override
    public Note save(Note note) {
        var sql = """
                INSERT INTO notes (user_id, title, content)
                VALUES (?, ?, ?) RETURNING note_id, created_at
                """;
        try (var conn = DatabaseConfig.getConnection();
             var ps   = conn.prepareStatement(sql)) {
            ps.setLong(1, note.getUserId());
            ps.setString(2, note.getTitle());
            ps.setString(3, note.getContent());
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    note.setNoteId(rs.getLong("note_id"));
                    note.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
                }
            }
            return note;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save note", e);
        }
    }

    @Override
    public List<Note> findByUser(long userId) {
        var sql = """
                SELECT note_id, user_id, title, content, created_at
                FROM notes WHERE user_id = ? ORDER BY created_at DESC
                """;
        try (var conn = DatabaseConfig.getConnection();
             var ps   = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (var rs = ps.executeQuery()) {
                var list = new ArrayList<Note>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch notes", e);
        }
    }

    @Override
    public void delete(long noteId) {
        try (var conn = DatabaseConfig.getConnection();
             var ps   = conn.prepareStatement("DELETE FROM notes WHERE note_id = ?")) {
            ps.setLong(1, noteId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete note", e);
        }
    }

    private Note mapRow(ResultSet rs) throws SQLException {
        var n = new Note();
        n.setNoteId(rs.getLong("note_id"));
        n.setUserId(rs.getLong("user_id"));
        n.setTitle(rs.getString("title"));
        n.setContent(rs.getString("content"));
        n.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        return n;
    }
}
