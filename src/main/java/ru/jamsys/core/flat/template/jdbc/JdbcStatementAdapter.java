package ru.jamsys.core.flat.template.jdbc;

import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class JdbcStatementAdapter {

    public void setInParam(PreparedStatement cs, ArgumentType type, int index, Object value) throws Exception {
        boolean isNull = value == null;
        if (isNull) {
            cs.setNull(index, type.getType());
            return;
        }

        String valueStr = String.valueOf(value);
        switch (type) {
            case VARCHAR -> cs.setString(index, valueStr);
            case NUMBER ->
                    cs.setBigDecimal(index, valueStr != null && !valueStr.isEmpty() ? new BigDecimal(valueStr) : null);
            case TIMESTAMP -> setInTimestamp(cs, index, valueStr);
        }
    }

    public void setInTimestamp(PreparedStatement cs, int index, String value) throws SQLException {
        if (!((value != null) && !value.isEmpty())) {
            cs.setTimestamp(index, null);
            return;
        }
        try {
            long timestampMills = Long.parseLong(value);
            LocalDateTime timestamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestampMills), ZoneId.systemDefault());
            cs.setTimestamp(index, Timestamp.valueOf(timestamp));
        } catch (NumberFormatException e) {
            cs.setTimestamp(index, Timestamp.valueOf(ZonedDateTime.parse(value).toLocalDateTime()));
        }
    }

    public void setOutParam(CallableStatement cs, ArgumentType type, int index) throws SQLException {
        cs.registerOutParameter(index, type.getType());
    }

    public Object getOutParam(CallableStatement cs, ArgumentType type, int index) throws Exception {
        return switch (type) {
            case VARCHAR -> cs.getString(index);
            case NUMBER -> cs.getBigDecimal(index);
            case TIMESTAMP -> cs.getTimestamp(index);
            default -> null;
        };
    }

    public Object getColumn(ResultSet rs, ArgumentType type, String name) throws Exception {
        return switch (type) {
            case VARCHAR -> rs.getString(name);
            case NUMBER -> rs.getBigDecimal(name);
            case TIMESTAMP -> rs.getTimestamp(name);
            default -> null;
        };
    }

}
