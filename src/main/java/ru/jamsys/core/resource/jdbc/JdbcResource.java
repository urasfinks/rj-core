package ru.jamsys.core.resource.jdbc;

import com.fasterxml.jackson.annotation.JsonValue;
import ru.jamsys.core.App;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.expiration.AbstractExpirationResource;
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

public class JdbcResource
        extends AbstractExpirationResource
        implements
        JdbcExecute,
        PropertyListener {

    private final StatementControl statementControl;

    private Connection connection;

    private final PropertyDispatcher<String> propertyDispatcher;

    private final JdbcRepositoryProperty property = new JdbcRepositoryProperty();

    private final String ns;

    public JdbcResource(String ns) {
        this.ns = ns;
        propertyDispatcher = new PropertyDispatcher<>(
                this,
                property,
                getCascadeKey(ns)
        );
        this.statementControl = new DefaultStatementControl();
    }

    private void up() {
        if (connection == null) {
            try {
                SecurityComponent securityComponent = App.get(SecurityComponent.class);
                this.connection = DriverManager.getConnection(
                        property.getUri(),
                        property.getUser(),
                        new String(securityComponent.get(property.getSecurityAlias()))
                );
            } catch (Throwable th) {
                throw new ForwardException(this, th);
            }
        }
    }

    private void down() {
        if (connection != null) {
            try {
                connection.close();
            } catch (Throwable th) {
                App.error(th, this);
            }
            connection = null;
        }
    }

    public List<Map<String, Object>> execute(JdbcRequest arguments) throws Throwable {
        if (connection == null) {
            throw new ForwardException("Connection is null", this);
        }
        JdbcTemplate jdbcTemplate = arguments.getJdbcTemplate();
        if (jdbcTemplate == null) {
            throw new ForwardException("TemplateEnum: " + arguments.getName() + " return null template", this);
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
                throw new ForwardException(this, th);
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

    @JsonValue
    public Object getJsonValue() {
        return new HashMapBuilder<>()
                .append("hashCode", Integer.toHexString(hashCode()))
                .append("cls", getClass())
                .append("ns", ns)
                .append("propertyDispatcherNs", propertyDispatcher.getNs())
                ;
    }

    @Override
    public void runOperation() {
        propertyDispatcher.run();
        up();
        if (connection == null) {
            throw new ForwardException("Connection problem", this);
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
        if (property.getUri() == null || property.getUser() == null || property.getSecurityAlias() == null) {
            return;
        }
        up();
    }

}
