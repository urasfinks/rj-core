package ru.jamsys.core.resource.jdbc;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ExceptionHandler;
import ru.jamsys.core.component.PropComponent;
import ru.jamsys.core.component.SecurityComponent;
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
public class JdbcResource
        extends ExpirationMsMutableImpl
        implements
        Resource<JdbcResourceConstructor, JdbcRequest, List<Map<String, Object>>>,
        JdbcExecute {

    private StatementControl statementControl;

    private Connection connection;

    private String uri;

    private String user;

    private String securityAlias;

    @Override
    public void constructor(JdbcResourceConstructor constructor) throws Exception {
        PropComponent propComponent = App.context.getBean(PropComponent.class);

        propComponent.getProp(constructor.ns, "jdbc.uri", s -> {this.uri = s; reInitClient();});
        propComponent.getProp(constructor.ns, "jdbc.user", s -> {this.user = s; reInitClient();});
        propComponent.getProp(constructor.ns, "jdbc.security.alias", s -> {this.securityAlias = s; reInitClient();});

        this.statementControl = constructor.getStatementControl();
    }

    private void reInitClient() {
        if (uri == null || user == null || securityAlias == null) {
            return;
        }
        if (connection != null) {
            close();
        }
        try {
            SecurityComponent securityComponent = App.context.getBean(SecurityComponent.class);
            this.connection = DriverManager.getConnection(uri, user, new String(securityComponent.get(securityAlias)));
        } catch (Exception e) {
            App.context.getBean(ExceptionHandler.class).handler(e);
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
            App.context.getBean(ExceptionHandler.class).handler(e);
        }
    }

    @Override
    public int getWeight(BalancerAlgorithm balancerAlgorithm) {
        return 0;
    }

}
