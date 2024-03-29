package ru.jamsys.pool;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.App;
import ru.jamsys.component.PropertiesManager;
import ru.jamsys.component.RateLimitManager;
import ru.jamsys.component.Security;
import ru.jamsys.jdbc.ConnectionEnvelope;
import ru.jamsys.rate.limit.RateLimitTps;
import ru.jamsys.template.jdbc.DefaultStatementControl;
import ru.jamsys.template.jdbc.StatementControl;

import java.sql.DriverManager;

public class JdbcPool extends AbstractPool<ConnectionEnvelope> {

    @Setter
    private String uri;

    @Setter
    private String user;

    @Setter
    private String securityAlias;

    @Getter
    private final StatementControl statementControl = new DefaultStatementControl();

    public JdbcPool(String name, int min, int max) {
        super(name, min, max);
        PropertiesManager propertiesManager = App.context.getBean(PropertiesManager.class);
        this.uri = propertiesManager.getProperties("rj.jdbc.uri", String.class);
        this.user = propertiesManager.getProperties("rj.jdbc.user", String.class);
        this.securityAlias = propertiesManager.getProperties("rj.jdbc.security.alias", String.class);
    }

    @SuppressWarnings("unused")
    public void setProperty(String uri, String user, Security security, String securityAlias) {
        this.uri = uri;
        this.user = user;
        this.securityAlias = securityAlias;
    }

    @Override
    public ConnectionEnvelope createResource() {
        try {
            Security security = App.context.getBean(Security.class);
            return new ConnectionEnvelope(
                    DriverManager.getConnection(uri, user, new String(security.get(securityAlias))),
                    this
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void closeResource(ConnectionEnvelope resource) {
        resource.close();
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

    public RateLimitTps getRateLimitThread() {
        return App.context.getBean(RateLimitManager.class)
                .get(ConnectionEnvelope.class, RateLimitTps.class, getName());
    }

    @Override
    public void run() {
        super.run();
        getRateLimitThread().setActive(true);
    }

    @Override
    public void shutdown() {
        super.shutdown();
        getRateLimitThread().setActive(false);
    }

}
