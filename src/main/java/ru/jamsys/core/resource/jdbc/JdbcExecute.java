package ru.jamsys.core.resource.jdbc;

import ru.jamsys.core.flat.template.jdbc.*;
import ru.jamsys.core.flat.util.UtilLog;

import java.sql.*;
import java.util.*;

public interface JdbcExecute {

    default List<Map<String, Object>> execute(
            Connection conn,
            SqlStatementDefinition sqlStatementDefinition,
            SqlArgumentBuilder argumentsBuilder,
            JdbcStatementAdapter jdbcStatementAdapter,
            boolean debug
    ) throws Exception {
        if (conn == null) {
            throw new RuntimeException("Connection is null");
        }
        List<Map<String, Object>> arguments = argumentsBuilder.get();
        List<Map<String, Object>> result = new ArrayList<>();
        if (arguments == null) {
            return result;
        }
        // Динамичный фрагмент не может использоваться в executeBatch
        if (sqlStatementDefinition.getSqlTemplateCompiler().isDynamicArgument() && arguments.size() > 1) {
            throw new RuntimeException("ExecuteBatch not support DynamicArguments");
        }
        SqlTemplateCompiled sqlTemplateCompiled = sqlStatementDefinition.getSqlTemplateCompiler().compile(arguments.getFirst());
        if (debug) {
            UtilLog.printInfo(sqlTemplateCompiled.getSql());
            UtilLog.printInfo(DebugVisualizer.get(sqlTemplateCompiled));
        }
        SqlExecutionMode sqlExecutionMode = sqlStatementDefinition.getSqlExecutionMode();
        conn.setAutoCommit(sqlExecutionMode.isAutoCommit());
        PreparedStatement preparedStatement =
                sqlExecutionMode.isSelect()
                        ? conn.prepareStatement(sqlTemplateCompiled.getSql())
                        : conn.prepareCall(sqlTemplateCompiled.getSql());
        if (arguments.size() == 1) {
            for (Argument argument : sqlTemplateCompiled.getListArgument()) {
                setParam(jdbcStatementAdapter, preparedStatement, argument);
            }
            preparedStatement.execute();
        } else if (!arguments.isEmpty()) {
            for (Map<String, Object> qArgs : arguments) {
                SqlTemplateCompiled tmp = sqlStatementDefinition.getSqlTemplateCompiler().compile(qArgs);
                for (Argument argument : tmp.getListArgument()) {
                    setParam(jdbcStatementAdapter, preparedStatement, argument);
                }
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
        }

        switch (sqlStatementDefinition.getSqlExecutionMode()) {
            case SELECT_WITHOUT_AUTO_COMMIT:
            case SELECT_WITH_AUTO_COMMIT:
                try (ResultSet resultSet = preparedStatement.getResultSet()) {
                    if (resultSet == null) {
                        return result;
                    }
                    Integer columnCount = null;
                    Map<Integer, String> cacheName = new HashMap<>();
                    while (resultSet.next()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        if (columnCount == null) {
                            ResultSetMetaData metaData = resultSet.getMetaData();
                            columnCount = metaData.getColumnCount();
                            for (int i = 1; i <= columnCount; i++) {
                                String name = metaData.getColumnName(i);
                                row.put(name, resultSet.getObject(i));
                                cacheName.put(i, name);
                            }
                        } else {
                            for (int i = 1; i <= columnCount; i++) {
                                row.put(cacheName.get(i), resultSet.getObject(i));
                            }
                        }
                        result.add(row);
                    }
                }
                break;
            case CALL_WITHOUT_AUTO_COMMIT:
            case CALL_WITH_AUTO_COMMIT:
                Map<String, Object> row = new LinkedHashMap<>();
                for (Argument argument : sqlTemplateCompiled.getListArgument()) {
                    ArgumentDirection direction = argument.getDirection();
                    if (direction == ArgumentDirection.OUT || direction == ArgumentDirection.IN_OUT) {
                        row.put(
                                argument.getKey(),
                                jdbcStatementAdapter.getOutParam(
                                        (CallableStatement) preparedStatement,
                                        argument.getType(),
                                        argument.getIndex())
                        );
                    }
                }
                result.add(row);
                break;
        }
        if (debug) {
            UtilLog.printInfo(result);
        }
        return result;
    }

    private static void setParam(
            JdbcStatementAdapter jdbcStatementAdapter,
            PreparedStatement preparedStatement,
            Argument arg
    ) throws Exception {
        switch (arg.getDirection()) {
            case IN ->
                    jdbcStatementAdapter.setInParam(preparedStatement, arg.getType(), arg.getIndex(), arg.getValue());
            case OUT ->
                    jdbcStatementAdapter.setOutParam((CallableStatement) preparedStatement, arg.getType(), arg.getIndex());
            case IN_OUT -> {
                jdbcStatementAdapter.setOutParam((CallableStatement) preparedStatement, arg.getType(), arg.getIndex());
                jdbcStatementAdapter.setInParam(preparedStatement, arg.getType(), arg.getIndex(), arg.getValue());
            }
        }
    }

}
