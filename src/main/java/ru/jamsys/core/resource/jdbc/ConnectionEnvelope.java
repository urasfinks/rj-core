package ru.jamsys.core.resource.jdbc;

import ru.jamsys.core.App;
import ru.jamsys.core.component.ExceptionHandler;
import ru.jamsys.core.component.api.RateLimitManager;
import ru.jamsys.core.pool.Pool;
import ru.jamsys.core.pool.PoolItem;
import ru.jamsys.core.rate.limit.RateLimit;
import ru.jamsys.core.rate.limit.RateLimitType;
import ru.jamsys.core.template.jdbc.*;
import ru.jamsys.core.util.Util;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConnectionEnvelope extends PoolItem<ConnectionEnvelope> {

    final private Connection connection;

    final private RateLimit rateLimit;

    private final AtomicBoolean reusable = new AtomicBoolean(false);

    //JdbcPool потому что надо получить контроллер sql операторов (pool.getStatementControl())
    public ConnectionEnvelope(Connection connection, Pool<ConnectionEnvelope> pool) {
        super(pool);
        this.connection = connection;
        rateLimit = App.context.getBean(RateLimitManager.class).get(getClass(), pool.getName());
        rateLimit.init(RateLimitType.POOL_ITEM_TPS);
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
    @SuppressWarnings("unused")
    public void startReusable() throws Exception {
        if (!rateLimit.check(null)) {
            stopReusable();
            throw new Exception("RateLimit overflow");
        }
        reusable.set(true);
    }

    public void stopReusable() {
        pool.complete(this, null);
    }

    @SuppressWarnings("unused")
    public List<Map<String, Object>> exec(JdbcRequest task) throws Exception {
        if (!rateLimit.check(null) && !reusable.get()) {
            // Если ресурс переиспользуемый - то это было сделано для того, что бы исполнить транзакцию состоящую
            // из нескольких запросов, и даже если tps закончились - надо, что бы транзакция завершилась commit
            throw new Exception("RateLimit overflow");
        }
        active();
        Template template = task.getTemplate();
        if (template == null) {
            complete(null);
            throw new Exception("TemplateEnum: " + task.getName() + " return null template");
        }
        try {
            List<Map<String, Object>> execute = execute(
                    connection,
                    template,
                    task.getArgs(),
                    ((JdbcPool) pool).getStatementControl(),
                    task.getDebug()
            );
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
            Util.logConsole(compiledSqlTemplate.getSql());
            Util.logConsole(template.debug(compiledSqlTemplate));
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
            case IN -> statementControl.setInParam(conn, preparedStatement, arg.getType(), arg.getIndex(), arg.getValue());
            case OUT -> statementControl.setOutParam((CallableStatement) preparedStatement, arg.getType(), arg.getIndex());
            case IN_OUT -> {
                statementControl.setOutParam((CallableStatement) preparedStatement, arg.getType(), arg.getIndex());
                statementControl.setInParam(conn, preparedStatement, arg.getType(), arg.getIndex(), arg.getValue());
            }
        }
    }

}