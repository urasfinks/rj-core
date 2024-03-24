package ru.jamsys.jdbc;

import ru.jamsys.App;
import ru.jamsys.component.ExceptionHandler;
import ru.jamsys.extension.AbstractPoolItem;
import ru.jamsys.pool.JdbcPool;
import ru.jamsys.statistic.RateLimitItem;
import ru.jamsys.template.jdbc.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConnectionEnvelope extends AbstractPoolItem {

    final private Connection connection;

    final private JdbcPool pool;

    final private RateLimitItem rateLimitItem;

    private final AtomicBoolean reusable = new AtomicBoolean(false);

    public ConnectionEnvelope(Connection connection, JdbcPool pool, RateLimitItem rateLimitItem) {
        this.connection = connection;
        this.pool = pool;
        this.rateLimitItem = rateLimitItem;
    }

    @Override
    public void polled() {
        reusable.set(false);
    }

    public void close() {
        try {
            connection.close();
        } catch (Exception e) {
            App.context.getBean(ExceptionHandler.class).handler(e);
        }
    }

    // Использование данного функционала должно быть очень аккуратным
    // Надо чётко обрабатывать все исключения, так как любой exception будет возвращать
    @SuppressWarnings("unused")
    public void startReusable() throws Exception {
        if (!rateLimitItem.checkTps()) {
            stopReusable();
            throw new Exception("Tps overflow. Max tps = " + rateLimitItem.getMax());
        }
        reusable.set(true);
    }

    public void stopReusable() {
        pool.complete(this, null);
    }

    @SuppressWarnings("unused")
    public List<Map<String, Object>> exec(TemplateEnum templateEnum, Map<String, Object> args, boolean debug) throws Exception {
        if (!rateLimitItem.checkTps() && !reusable.get()) {
            // Если ресурс переиспользуемый - то это было сделано для того, что бы исполнить транзакцию состоящую
            // из нескольких запросов, и даже если tps закончились - надо, что бы транзакция завершилась commit
            throw new Exception("Tps overflow. Max tps = " + rateLimitItem.getMax());
        }
        Template template = templateEnum.getTemplate();
        if (template == null) {
            complete(null);
            throw new Exception("TemplateEnum: " + templateEnum + " return null template");
        }
        try {
            List<Map<String, Object>> execute = execute(connection, template, args, pool.getStatementControl(), debug);
            complete(null);
            return execute;
        } catch (Exception e) {
            complete(e);
            App.context.getBean(ExceptionHandler.class).handler(e);
            throw e;
        }
    }

    public void complete(Exception e) {
        if (!reusable.get()) {
            pool.complete(this, e);
        }
    }

    private static List<Map<String, Object>> execute(
            Connection conn,
            Template template,
            Map<String, Object> args,
            StatementControl statementControl,
            boolean debug
    ) throws Exception {
        CompiledSqlTemplate compiledSqlTemplate = template.compile(args);
        if (debug) {
            System.out.println(compiledSqlTemplate.getSql());
            System.out.println(template.debug(compiledSqlTemplate));
        }
        StatementType statementType = template.getStatementType();
        conn.setAutoCommit(statementType.isAutoCommit());
        PreparedStatement preparedStatement =
                statementType.isSelect()
                        ? conn.prepareStatement(compiledSqlTemplate.getSql())
                        : conn.prepareCall(compiledSqlTemplate.getSql());
        for (Argument argument : compiledSqlTemplate.getListArgument()) {
            setParam(statementControl, conn, preparedStatement, argument);
        }
        preparedStatement.execute();
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
                        Map<String, Object> row = new HashMap<>();
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
                Map<String, Object> row = new HashMap<>();
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
