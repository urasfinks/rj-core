package ru.jamsys.core.resource.jdbc;

import ru.jamsys.core.App;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.expiration.AbstractExpirationResource;
import ru.jamsys.core.extension.log.DataHeader;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.extension.property.PropertyListener;
import ru.jamsys.core.flat.template.jdbc.DataMapper;
import ru.jamsys.core.flat.template.jdbc.DefaultStatementControl;
import ru.jamsys.core.flat.template.jdbc.JdbcTemplate;
import ru.jamsys.core.flat.template.jdbc.StatementControl;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class JdbcResource
        extends AbstractExpirationResource
        implements
        JdbcExecute,
        PropertyListener {

    private final StatementControl statementControl;

    private Connection connection;

    private final PropertyDispatcher<String> propertyDispatcher;

    private final JdbcRepositoryProperty jdbcRepositoryProperty = new JdbcRepositoryProperty();

    public JdbcResource(String ns) {
        propertyDispatcher = new PropertyDispatcher<>(
                this,
                jdbcRepositoryProperty,
                getCascadeKey(ns)
        );
        this.statementControl = new DefaultStatementControl();
    }

    private void up() {
        if (connection == null) {
            try {
                SecurityComponent securityComponent = App.get(SecurityComponent.class);
                this.connection = DriverManager.getConnection(
                        jdbcRepositoryProperty.getUri(),
                        jdbcRepositoryProperty.getUser(),
                        new String(securityComponent.get(jdbcRepositoryProperty.getSecurityAlias()))
                );
            } catch (Throwable th) {
                throw new ForwardException(th);
            }
        }
    }

    private void down() {
        if (connection != null) {
            try {
                connection.close();
            } catch (Throwable th) {
                App.error(new ForwardException(th));
            }
            connection = null;
        }
    }

    public List<Map<String, Object>> execute(JdbcRequest arguments) throws Throwable {
        if (connection == null) {
            throw new RuntimeException("Connection is null");
        }
        JdbcTemplate jdbcTemplate = arguments.getJdbcTemplate();
        if (jdbcTemplate == null) {
            throw new RuntimeException("TemplateEnum: " + arguments.getName() + " return null template");
        }
        return execute(connection, jdbcTemplate, arguments.getListArgs(), statementControl, arguments.isDebug());
    }

    @Override
    public boolean isValid() {
        if (connection == null) { // Явно если коннект null - всё плохо
            return false;
        }
        try {
            // Как будто проще проверить сначала на то что коннект закрыли сначала
            if (connection.isClosed()) { // Если коннект закрыт - всё плохо
                return false;
            }
            if (!connection.isValid(1)) { // Если коннект не валидный - всё плохо
                return false;
            }
        } catch (Throwable th) { // Любое исключение - всё плохо, в топку такой коннект
            return false;
        }
        return true;
    }

    public <T extends DataMapper<T>> List<T> execute(JdbcRequest arguments, Class<T> cls) throws Throwable {
        if (connection == null) {
            throw new RuntimeException("Connection is null");
        }
        JdbcTemplate jdbcTemplate = arguments.getJdbcTemplate();
        if (jdbcTemplate == null) {
            throw new RuntimeException("TemplateEnum: " + arguments.getName() + " return null template");
        }
        List<Map<String, Object>> execute = execute(connection, jdbcTemplate, arguments.getListArgs(), statementControl, arguments.isDebug());
        List<T> result = new ArrayList<>();
        execute.forEach(map -> {
            try {
                T t = cls.getDeclaredConstructor().newInstance();
                result.add(t.fromMap(map, DataMapper.TransformCodeStyle.SNAKE_TO_CAMEL));
            } catch (Throwable th) {
                throw new RuntimeException(th);
            }
        });
        return result;
    }

    @Override
    public boolean checkFatalException(Throwable th) {
        if (th != null) {
            String msg = th.getMessage();
                if (msg == null) {
                    App.error(th);
                    return false;
                }
                // Не конкурентная проверка
                return msg.contains("закрыто")
                        || msg.contains("close")
                        || msg.contains("Connection reset")
                        || msg.contains("Connection is null")
                        || msg.contains("Connection problem")
                        || msg.contains("terminating connection")
                        || msg.contains("Ошибка ввода/вывода при отправке бэкенду");
            }
            return false;
    }

    @Override
    public void runOperation() {
        propertyDispatcher.run();
        up();
        if (connection == null) {
            throw new RuntimeException("Connection problem");
        }
    }

    @Override
    public void shutdownOperation() {
        propertyDispatcher.shutdown();
        down();
    }

    @Override
    public void onPropertyUpdate(String key, String oldValue, String newValue) {
        down();
        if (jdbcRepositoryProperty.getUri() == null || jdbcRepositoryProperty.getUser() == null || jdbcRepositoryProperty.getSecurityAlias() == null) {
            return;
        }
        up();
    }

    @Override
    public List<DataHeader> flushAndGetStatistic(AtomicBoolean threadRun) {
        return List.of();
    }

}
