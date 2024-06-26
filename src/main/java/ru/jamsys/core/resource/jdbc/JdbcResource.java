package ru.jamsys.core.resource.jdbc;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.property.PropertySubscriberNotify;
import ru.jamsys.core.extension.property.Subscriber;
import ru.jamsys.core.flat.template.jdbc.DefaultStatementControl;
import ru.jamsys.core.flat.template.jdbc.StatementControl;
import ru.jamsys.core.flat.template.jdbc.TemplateJdbc;
import ru.jamsys.core.resource.NamespaceResourceConstructor;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.resource.balancer.algorithm.BalancerAlgorithm;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Component
@Scope("prototype")
public class JdbcResource
        extends ExpirationMsMutableImpl
        implements
        Resource<JdbcRequest, List<Map<String, Object>>>,
        JdbcExecute,
        PropertySubscriberNotify {

    private StatementControl statementControl;

    private Connection connection;

    private Subscriber subscriber;

    private final JdbcProperty property = new JdbcProperty();

    @Override
    public void constructor(NamespaceResourceConstructor constructor) throws Exception {
        ServiceProperty serviceProperty = App.get(ServiceProperty.class);
        subscriber = serviceProperty.getSubscriber(this, property, constructor.ns);
        this.statementControl = new DefaultStatementControl();
    }

    @Override
    public void onPropertyUpdate(Set<String> updatedProp) {
        if (property.getUri() == null || property.getUser() == null || property.getSecurityAlias() == null) {
            return;
        }
        if (connection != null) {
            close();
        }
        try {
            SecurityComponent securityComponent = App.get(SecurityComponent.class);
            this.connection = DriverManager.getConnection(
                    property.getUri(),
                    property.getUser(),
                    new String(securityComponent.get(property.getSecurityAlias()))
            );
        } catch (Exception e) {
            App.error(e);
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
    public void close() {
        if (subscriber != null) {
            subscriber.unsubscribe();
        }
        try {
            connection.close();
        } catch (Exception e) {
            App.error(e);
        }
    }

    @Override
    public int getWeight(BalancerAlgorithm balancerAlgorithm) {
        return 0;
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

}
