package ru.jamsys.core.flat.template.jdbc;

import ru.jamsys.core.flat.util.UtilTypeConverter;

import java.sql.*;

public class JdbcStatementAdapter {

    public void setInParam(PreparedStatement cs, ArgumentType type, int index, Object value) throws Exception {
        if (value == null) {
            cs.setNull(index, type.getType());
            return;
        }

        switch (type) {
            case VARCHAR -> cs.setString(index, value.toString());
            case NUMBER -> cs.setBigDecimal(index, UtilTypeConverter.toBigDecimal(value));
            case TIMESTAMP -> cs.setTimestamp(index, UtilTypeConverter.toTimestamp(value));
            case BOOLEAN -> cs.setBoolean(index, Boolean.TRUE.equals(UtilTypeConverter.toBoolean(value)));
            case ARRAY -> {
                if (!(value instanceof Array array)) {
                    throw new IllegalArgumentException("Expected java.sql.Array but got: " + value.getClass());
                }
                cs.setArray(index, array);
            }
            default -> throw new IllegalArgumentException("Unsupported ArgumentType: " + type);
        }
    }

    public void setOutParam(CallableStatement cs, ArgumentType type, int index) throws SQLException {
        cs.registerOutParameter(index, type.getType());
    }

    public Object getOutParam(CallableStatement cs, ArgumentType type, int index) throws SQLException {
        return switch (type) {
            case VARCHAR -> cs.getString(index);
            case NUMBER -> cs.getBigDecimal(index);
            case TIMESTAMP -> cs.getTimestamp(index);
            case BOOLEAN -> cs.getBoolean(index);
            case ARRAY -> cs.getArray(index);
            default -> throw new IllegalArgumentException("Unsupported ArgumentType: " + type);
        };
    }

    @SuppressWarnings("unused")
    public Object getColumn(ResultSet rs, ArgumentType type, String name) throws SQLException {
        return switch (type) {
            case VARCHAR -> rs.getString(name);
            case NUMBER -> rs.getBigDecimal(name);
            case TIMESTAMP -> rs.getTimestamp(name);
            case BOOLEAN -> rs.getBoolean(name);
            case ARRAY -> rs.getArray(name);
            default -> throw new IllegalArgumentException("Unsupported ArgumentType: " + type);
        };
    }

}
