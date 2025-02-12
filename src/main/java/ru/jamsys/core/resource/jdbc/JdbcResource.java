package ru.jamsys.core.resource.jdbc;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.property.PropertiesAgent;
import ru.jamsys.core.extension.property.PropertyUpdateDelegate;
import ru.jamsys.core.flat.template.jdbc.DataMapper;
import ru.jamsys.core.flat.template.jdbc.DefaultStatementControl;
import ru.jamsys.core.flat.template.jdbc.JdbcTemplate;
import ru.jamsys.core.flat.template.jdbc.StatementControl;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.resource.ResourceArguments;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
@Scope("prototype")
public class JdbcResource
        extends ExpirationMsMutableImpl
        implements
        Resource<JdbcRequest, List<Map<String, Object>>>,
        JdbcExecute,
        PropertyUpdateDelegate {

    private StatementControl statementControl;

    private Connection connection;

    private PropertiesAgent propertiesAgent;

    private final JdbcProperties property = new JdbcProperties();

    @Override
    public void setArguments(ResourceArguments resourceArguments) throws Exception {
        ServiceProperty serviceProperty = App.get(ServiceProperty.class);
        propertiesAgent = serviceProperty.getFactory().getPropertiesAgent(
                this,
                property,
                resourceArguments.ns,
                true
        );
        this.statementControl = new DefaultStatementControl();
    }

    @Override
    public void onPropertyUpdate(Map<String, String> mapAlias) {
        down();
        if (property.getUri() == null || property.getUser() == null || property.getSecurityAlias() == null) {
            return;
        }
        up();
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

    @Override
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
            // Как будто проще проверить сначала на то что коннект закрыли санчала
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
                result.add(t.fromMap(map, DataMapper.Transform.SNAKE_TO_CAMEL));
            } catch (Throwable th) {
                throw new RuntimeException(th);
            }
        });
        return result;
    }

    @Override
    public Function<Throwable, Boolean> getFatalException() {
        return throwable -> {
            if (throwable != null) {
                String msg = throwable.getMessage();
                if (msg == null) {
                    App.error(throwable);
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
        };
    }

    @Override
    public void run() {
        if (propertiesAgent != null) {
            propertiesAgent.run();
        }
        up();
        if (connection == null) {
            throw new RuntimeException("Connection problem");
        }
    }

    @Override
    public void shutdown() {
        if (propertiesAgent != null) {
            propertiesAgent.shutdown();
        }
        down();
    }

}
