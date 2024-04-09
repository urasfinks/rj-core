package ru.jamsys.notification;

import lombok.Setter;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.HtmlEmail;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.App;
import ru.jamsys.component.PropertiesManager;
import ru.jamsys.component.Security;
import ru.jamsys.http.JsonHttpResponse;

@SuppressWarnings("unused")
@Component
@Lazy
public class NotificationEmail implements Notification {

    private final Security security;

    @Setter
    private String host;

    @Setter
    private String user;

    @Setter
    private String from;

    @Setter
    private String charset;

    @Setter
    private String securityAlias;

    @Setter
    private int port;

    @Setter
    private int connectTimeoutMs;

    @Setter
    private boolean ssl;

    public NotificationEmail(Security security, PropertiesManager propertiesManager) {

        this.security = security;

        this.host = propertiesManager.getProperties("rj.notification.email.host", String.class);
        this.user = propertiesManager.getProperties("rj.notification.email.user", String.class);
        this.from = propertiesManager.getProperties("rj.notification.email.from", String.class);
        this.charset = propertiesManager.getProperties("rj.notification.email.charset", String.class);
        this.securityAlias = propertiesManager.getProperties("rj.notification.email.security.alias", String.class);

        this.port = propertiesManager.getProperties("rj.notification.email.port", Integer.class);
        this.connectTimeoutMs = propertiesManager.getProperties("rj.notification.email.connectTimeoutMs", Integer.class);

        this.ssl = propertiesManager.getProperties("rj.notification.email.ssl", Boolean.class);

    }

    @Override
    public JsonHttpResponse notify(String title, Object data, String to) {
        JsonHttpResponse jRet = new JsonHttpResponse();
        HtmlEmail email = new HtmlEmail();
        try {
            setting(email);
        } catch (Exception e) {
            jRet.addException(e);
        }
        return jRet;
    }

    @Override
    public Notification getInstance() {
        Security security = App.context.getBean(Security.class);
        PropertiesManager propertiesManager = App.context.getBean(PropertiesManager.class);
        return new NotificationEmail(security, propertiesManager);
    }

    private void setting(HtmlEmail email) throws Exception {
        email.setHostName(host);
        email.setSmtpPort(port);
        email.setAuthenticator(new DefaultAuthenticator(user, new String(security.get(securityAlias))));
        email.setSSLOnConnect(ssl);
        email.setFrom(from);
        email.setCharset(charset);
        email.setSocketConnectionTimeout(connectTimeoutMs);
        email.setSocketTimeout(connectTimeoutMs);
    }

}
