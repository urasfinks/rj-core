package ru.jamsys.core.resource.jdbc;

import ru.jamsys.core.flat.template.jdbc.*;
import ru.jamsys.core.flat.util.Util;

import java.sql.*;
import java.util.*;

public interface JdbcExecute {

    default List<Map<String, Object>> execute(
            Connection conn,
            TemplateJdbc template,
            List<Map<String, Object>> argsList,
            StatementControl statementControl,
            boolean debug
    ) throws Exception {
        // Динамичный фрагмент не может использоваться в executeBatch
        if (template.isDynamicArgument() && argsList.size() > 1) {
            throw new Exception("ExecuteBatch not support DynamicArguments");
        }
        CompiledSqlTemplate compiledSqlTemplate = template.compile(argsList.getFirst());
        if (debug) {
            Util.logConsole(compiledSqlTemplate.getSql());
            Util.logConsole(template.debug(compiledSqlTemplate));
        }
        StatementType statementType = template.getStatementType();
        conn.setAutoCommit(statementType.isAutoCommit());
        PreparedStatement preparedStatement =
                statementType.isSelect()
                        ? conn.prepareStatement(compiledSqlTemplate.getSql())
                        : conn.prepareCall(compiledSqlTemplate.getSql());
        if (argsList.size() == 1) {
            for (Argument argument : compiledSqlTemplate.getListArgument()) {
                setParam(statementControl, conn, preparedStatement, argument);
            }
            preparedStatement.execute();
        } else if (!argsList.isEmpty()) {
            for (Map<String, Object> qArgs : argsList) {
                CompiledSqlTemplate tmp = template.compile(qArgs);
                for (Argument argument : tmp.getListArgument()) {
                    setParam(statementControl, conn, preparedStatement, argument);
                }
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
        }

        List<Map<String, Object>> listRet = new ArrayList<>();
        switch (template.getStatementType()) {
            case SELECT_WITHOUT_AUTO_COMMIT:
            case SELECT_WITH_AUTO_COMMIT:
                try (ResultSet resultSet = preparedStatement.getResultSet()) {
                    if (resultSet == null) {
                        return listRet;
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
                        listRet.add(row);
                    }
                }
                break;
            case CALL_WITHOUT_AUTO_COMMIT:
            case CALL_WITH_AUTO_COMMIT:
                Map<String, Object> row = new LinkedHashMap<>();
                for (Argument argument : compiledSqlTemplate.getListArgument()) {
                    ArgumentDirection direction = argument.getDirection();
                    if (direction == ArgumentDirection.OUT || direction == ArgumentDirection.IN_OUT) {
                        row.put(
                                argument.getKey(),
                                statementControl.getOutParam(
                                        (CallableStatement) preparedStatement,
                                        argument.getType(),
                                        argument.getIndex())
                        );
                    }
                }
                listRet.add(row);
                break;
        }
        return listRet;
    }

    private static void setParam(
            StatementControl statementControl,
            Connection conn,
            PreparedStatement preparedStatement,
            Argument arg
    ) throws Exception {
        switch (arg.getDirection()) {
            case IN ->
                    statementControl.setInParam(conn, preparedStatement, arg.getType(), arg.getIndex(), arg.getValue());
            case OUT ->
                    statementControl.setOutParam((CallableStatement) preparedStatement, arg.getType(), arg.getIndex());
            case IN_OUT -> {
                statementControl.setOutParam((CallableStatement) preparedStatement, arg.getType(), arg.getIndex());
                statementControl.setInParam(conn, preparedStatement, arg.getType(), arg.getIndex(), arg.getValue());
            }
        }
    }

}
