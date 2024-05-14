package ru.jamsys.core.resource.jdbc;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.App;
import ru.jamsys.core.balancer.algorithm.BalancerAlgorithm;
import ru.jamsys.core.component.ExceptionHandler;
import ru.jamsys.core.component.Security;
import ru.jamsys.core.component.resource.PropertiesComponent;
import ru.jamsys.core.extension.Closable;
import ru.jamsys.core.extension.ResourcePromiseTask;
import ru.jamsys.core.pool.AbstractPool;
import ru.jamsys.core.promise.PromiseTask;
import ru.jamsys.core.template.jdbc.DefaultStatementControl;
import ru.jamsys.core.template.jdbc.StatementControl;

import java.sql.DriverManager;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class JdbcPool extends AbstractPool<ConnectionResource>
        implements Closable, ResourcePromiseTask<List<Map<String, Object>>, JdbcRequest> {

    @Getter
    private final StatementControl statementControl = new DefaultStatementControl();
    @Setter
    private String uri;
    @Setter
    private String user;
    @Setter
    private String securityAlias;

    public JdbcPool(String name, int min) {
        super(name, min, ConnectionResource.class);
        PropertiesComponent propertiesComponent = App.context.getBean(PropertiesComponent.class);
        this.uri = propertiesComponent.getProperties("rj.jdbc.uri", String.class);
        this.user = propertiesComponent.getProperties("rj.jdbc.user", String.class);
        this.securityAlias = propertiesComponent.getProperties("rj.jdbc.security.alias", String.class);
    }

    public void setProperty(String uri, String user, Security security, String securityAlias) {
        this.uri = uri;
        this.user = user;
        this.securityAlias = securityAlias;
    }

    @Override
    public ConnectionResource createPoolItem() {
        try {
            Security security = App.context.getBean(Security.class);
            return new ConnectionResource(
                    DriverManager.getConnection(uri, user, new String(security.get(securityAlias))),
                    this
            );
        } catch (Exception e) {
            App.context.getBean(ExceptionHandler.class).handler(e);
        }
        return null;
    }

    @Override
    public void closePoolItem(ConnectionResource connectionResource) {
        connectionResource.close();
    }

    @Override
    public boolean checkExceptionOnComplete(Exception e) {
        if (e != null) {
            String msg = e.getMessage();
            return msg.contains("закрыто")
                    || msg.contains("close")
                    || msg.contains("Connection reset")
                    || msg.contains("Ошибка ввода/вывода при отправке бэкенду");
        }
        return false;
    }

    @Override
    public void close() {
        shutdown();
    }

    @Override
    public int getWeight(BalancerAlgorithm balancerAlgorithm) {
        return 0;
    }

    @Override
    public List<Map<String, Object>> execute(JdbcRequest arguments) {
        ConnectionResource poolItem = getPoolItem();
        if (poolItem != null) {
            return poolItem.execute(arguments);
        }
        return null;
    }

    @Override
    public void executeAsync(JdbcRequest arguments, PromiseTask promiseTask) {

    }
}
