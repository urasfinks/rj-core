package ru.jamsys.core.resource.jdbc;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ExceptionHandler;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.component.PropertiesComponent;
import ru.jamsys.core.extension.Closable;
import ru.jamsys.core.pool.AbstractPool;
import ru.jamsys.core.flat.template.jdbc.DefaultStatementControl;
import ru.jamsys.core.flat.template.jdbc.StatementControl;

import java.sql.DriverManager;
import java.util.List;
import java.util.Map;

public class JdbcPool extends AbstractPool<JdbcRequest, List<Map<String, Object>>, ConnectionResource>
        implements Closable {

    @Getter
    private final StatementControl statementControl = new DefaultStatementControl();
    @Setter
    private String uri;
    @Setter
    private String user;
    @Setter
    private String securityAlias;

    public JdbcPool(String name) {
        super(name, ConnectionResource.class);
        PropertiesComponent propertiesComponent = App.context.getBean(PropertiesComponent.class);
        this.uri = propertiesComponent.getProperties("rj.jdbc.uri", String.class);
        this.user = propertiesComponent.getProperties("rj.jdbc.user", String.class);
        this.securityAlias = propertiesComponent.getProperties("rj.jdbc.security.alias", String.class);
    }

    public void setProperty(String uri, String user, String securityAlias) {
        this.uri = uri;
        this.user = user;
        this.securityAlias = securityAlias;
    }

    @Override
    public ConnectionResource createPoolItem() {
        try {
            SecurityComponent securityComponent = App.context.getBean(SecurityComponent.class);
            return new ConnectionResource(
                    DriverManager.getConnection(uri, user, new String(securityComponent.get(securityAlias))),
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
            // Не конкурентная проверка
            return msg.contains("закрыто")
                    || msg.contains("close")
                    || msg.contains("Connection reset")
                    || msg.contains("Ошибка ввода/вывода при отправке бэкенду");
        }
        return false;
    }

    @Override
    public void onParkUpdate() {

    }

    @Override
    public void close() {
        shutdown();
    }

}
