package ru.jamsys.core.resource.jdbc;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.PropertyComponent;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.extension.property.PropertySubscriberNotify;
import ru.jamsys.core.extension.property.Subscriber;
import ru.jamsys.core.flat.template.jdbc.StatementControl;
import ru.jamsys.core.flat.template.jdbc.TemplateJdbc;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.resource.balancer.algorithm.BalancerAlgorithm;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@Scope("prototype")
public class JdbcResource
        extends ExpirationMsMutableImpl
        implements
        Resource<JdbcResourceConstructor, JdbcRequest, List<Map<String, Object>>>,
        JdbcExecute,
        PropertySubscriberNotify {

    private StatementControl statementControl;

    private Connection connection;

    private Subscriber subscriber;

    private final JdbcProperty property = new JdbcProperty();

    @Override
    public void constructor(JdbcResourceConstructor constructor) throws Exception {
        PropertyComponent propertyComponent = App.get(PropertyComponent.class);
        subscriber = propertyComponent.getSubscriber(this, property, constructor.ns);
        this.statementControl = constructor.getStatementControl();
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
        return execute(connection, template, arguments.getArgs(), statementControl, arguments.getDebug());
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (Exception e) {
            App.error(e);
        }
        subscriber.unsubscribe();
    }

    @Override
    public int getWeight(BalancerAlgorithm balancerAlgorithm) {
        return 0;
    }

}
