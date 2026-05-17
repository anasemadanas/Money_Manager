package com.moneymanager.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

final class JdbcDates {

    private JdbcDates() {}

    static OffsetDateTime getOffsetDateTime(ResultSet rs, String column) throws SQLException {
        Object val = rs.getObject(column);
        if (val == null) return null;
        if (val instanceof OffsetDateTime odt) return odt;
        if (val instanceof Timestamp ts) return OffsetDateTime.ofInstant(ts.toInstant(), ZoneOffset.UTC);
        if (val instanceof Instant i) return OffsetDateTime.ofInstant(i, ZoneOffset.UTC);
        if (val instanceof String s) return OffsetDateTime.parse(s);
        return OffsetDateTime.parse(String.valueOf(val));
    }
}

