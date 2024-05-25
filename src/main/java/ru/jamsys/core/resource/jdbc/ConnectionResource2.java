package ru.jamsys.core.resource.jdbc;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ExceptionHandler;
import ru.jamsys.core.component.PropertiesComponent;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.extension.Completable;
import ru.jamsys.core.flat.template.jdbc.StatementControl;
import ru.jamsys.core.flat.template.jdbc.TemplateJdbc;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.resource.balancer.algorithm.BalancerAlgorithm;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Map;

@Component
@Scope("prototype")
public class ConnectionResource2
        extends ExpirationMsMutableImpl
        implements
        Completable,
        Resource<JdbcPoolSetting, JdbcRequest, List<Map<String, Object>>>,
        JdbcExecute {

    private StatementControl statementControl;

    private Connection connection;

    @Override
    public void constructor(JdbcPoolSetting constructor) throws Exception {
        PropertiesComponent propertiesComponent = App.context.getBean(PropertiesComponent.class);
        String uri = propertiesComponent.getProperties(constructor.namespaceProperties, "jdbc.uri", String.class);
        String user = propertiesComponent.getProperties(constructor.namespaceProperties, "jdbc.user", String.class);
        String securityAlias = propertiesComponent.getProperties(constructor.namespaceProperties, "jdbc.security.alias", String.class);
        SecurityComponent securityComponent = App.context.getBean(SecurityComponent.class);
        this.connection = DriverManager.getConnection(uri, user, new String(securityComponent.get(securityAlias)));
        this.statementControl = constructor.statementControl;
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
            App.context.getBean(ExceptionHandler.class).handler(e);
        }
    }

    @Override
    public int getWeight(BalancerAlgorithm balancerAlgorithm) {
        return 0;
    }

}
