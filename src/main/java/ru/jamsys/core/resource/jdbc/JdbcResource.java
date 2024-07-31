package ru.jamsys.core.resource.jdbc;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.property.PropertiesAgent;
import ru.jamsys.core.extension.property.PropertyUpdateDelegate;
import ru.jamsys.core.flat.template.jdbc.DefaultStatementControl;
import ru.jamsys.core.flat.template.jdbc.StatementControl;
import ru.jamsys.core.flat.template.jdbc.TemplateJdbc;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.resource.ResourceArguments;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;

import java.sql.Connection;
import java.sql.DriverManager;
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
        propertiesAgent = serviceProperty.getFactory().getPropertiesAgentField(
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
                App.error(new ForwardException(th));
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
        TemplateJdbc template = arguments.getTemplate();
        if (template == null) {
            throw new RuntimeException("TemplateEnum: " + arguments.getName() + " return null template");
        }
        return execute(connection, template, arguments.getListArgs(), statementControl, arguments.getDebug());
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
    }

    @Override
    public void shutdown() {
        if (propertiesAgent != null) {
            propertiesAgent.shutdown();
        }
        down();
    }

}
