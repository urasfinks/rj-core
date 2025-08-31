package ru.jamsys.core.resource.jdbc;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.expiration.AbstractExpirationResource;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.extension.property.PropertyListener;
import ru.jamsys.core.flat.template.jdbc.ArgumentType;
import ru.jamsys.core.flat.template.jdbc.DataMapper;
import ru.jamsys.core.flat.template.jdbc.JdbcStatementAdapter;
import ru.jamsys.core.flat.template.jdbc.SqlStatementDefinition;

import java.sql.Array;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JdbcResource
        extends AbstractExpirationResource
        implements
        JdbcExecute,
        PropertyListener {

    private final JdbcStatementAdapter jdbcStatementAdapter;

    private Connection connection;

    private final PropertyDispatcher<String> propertyDispatcher;

    private final JdbcRepositoryProperty property = new JdbcRepositoryProperty();

    private final String ns;

    @Getter
    private final String key;

    public JdbcResource(String ns, String key) {
        this.ns = ns;
        this.key = key;
        propertyDispatcher = new PropertyDispatcher<>(
                this,
                property,
                getCascadeKey(ns)
        );
        // Если когда-нибудь появятся ещё БД, можно будет через Property выбирать другой класс работы с statement
        this.jdbcStatementAdapter = new JdbcStatementAdapter();
    }

    @SuppressWarnings("unused")
    public Array createArray(ArgumentType argumentType, Object[] javaArray) throws SQLException {
        return connection.createArrayOf(argumentType.getTypeName(), javaArray);
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

    public List<Map<String, Object>> execute(
            SqlStatementDefinition sqlStatementDefinition,
            SqlArgumentBuilder sqlArgumentBuilder
    ) throws Throwable {
        return execute(sqlStatementDefinition, sqlArgumentBuilder, false);
    }

    public List<Map<String, Object>> execute(
            SqlStatementDefinition sqlStatementDefinition,
            SqlArgumentBuilder sqlArgumentBuilder,
            boolean debug
    ) throws Throwable {
        if (connection == null) {
            throw new ForwardException("Connection is null", this);
        }
        if (sqlStatementDefinition == null) {
            throw new ForwardException(new HashMapBuilder<String, Object>()
                    .append("sqlTemplateCompiler", null)
                    .append("sqlArgumentBuilder", sqlArgumentBuilder)
                    .append("self", this));
        }
        return execute(
                connection,
                sqlStatementDefinition,
                sqlArgumentBuilder,
                jdbcStatementAdapter,
                debug
        );
    }

    public <T extends DataMapper<T>> List<T> execute(
            SqlStatementDefinition sqlStatementDefinition,
            SqlArgumentBuilder sqlArgumentBuilder,
            Class<T> cls
    ) throws Throwable {
        return execute(sqlStatementDefinition, sqlArgumentBuilder, cls, false);
    }

    public <T extends DataMapper<T>> List<T> execute(
            SqlStatementDefinition sqlStatementDefinition,
            SqlArgumentBuilder sqlArgumentBuilder,
            Class<T> cls,
            boolean debug
    ) throws Throwable {
        if (connection == null) {
            throw new RuntimeException("Connection is null");
        }
        if (sqlStatementDefinition == null) {
            throw new ForwardException(new HashMapBuilder<String, Object>()
                    .append("sqlTemplateCompiler", null)
                    .append("sqlArgumentBuilder", sqlArgumentBuilder)
                    .append("self", this));
        }
        List<Map<String, Object>> execute = execute(
                connection,
                sqlStatementDefinition,
                sqlArgumentBuilder,
                jdbcStatementAdapter,
                debug
        );
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
