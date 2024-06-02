package ru.jamsys.core.resource.notification.email;

import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.HtmlEmail;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.PropertiesComponent;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.resource.NamespaceResourceConstructor;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.resource.balancer.algorithm.BalancerAlgorithm;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;

@Component
public class EmailNotificationResource
        extends ExpirationMsMutableImpl
        implements Resource<NamespaceResourceConstructor, EmailNotificationRequest, Void> {

    private SecurityComponent securityComponent;

    private String host;

    private String user;

    private String from;

    private String charset;

    private String securityAlias;

    private int port;

    private int connectTimeoutMs;

    private boolean ssl;

    @Override
    public void constructor(NamespaceResourceConstructor constructor) throws Throwable {
        PropertiesComponent propertiesComponent = App.context.getBean(PropertiesComponent.class);
        securityComponent = App.context.getBean(SecurityComponent.class);

        this.host = propertiesComponent.getProperties(constructor.namespaceProperties, "notification.email.host", String.class);
        this.user = propertiesComponent.getProperties(constructor.namespaceProperties, "notification.email.user", String.class);
        this.from = propertiesComponent.getProperties(constructor.namespaceProperties, "notification.email.from", String.class);
        this.charset = propertiesComponent.getProperties(constructor.namespaceProperties, "notification.email.charset", String.class);
        this.securityAlias = propertiesComponent.getProperties(constructor.namespaceProperties, "notification.email.security.alias", String.class);

        this.port = propertiesComponent.getProperties(constructor.namespaceProperties, "notification.email.port", Integer.class);
        this.connectTimeoutMs = propertiesComponent.getProperties(constructor.namespaceProperties, "notification.email.connectTimeoutMs", Integer.class);

        this.ssl = propertiesComponent.getProperties(constructor.namespaceProperties, "notification.email.ssl", Boolean.class);
    }

    @Override
    public Void execute(EmailNotificationRequest arguments) {
        HtmlEmail email = new HtmlEmail();
        try {
            setting(email);
            email.addTo(arguments.getTo());
            email.setSubject(arguments.getTitle());
            if (arguments.getData() != null) {
                email.setTextMsg(arguments.getData());
            }
            email.setHtmlMsg(arguments.getDataHtml());
            email.send();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private void setting(HtmlEmail email) throws Exception {
        email.setHostName(host);
        email.setSmtpPort(port);
        email.setAuthenticator(new DefaultAuthenticator(user, new String(securityComponent.get(securityAlias))));
        email.setSSLOnConnect(ssl);
        email.setFrom(from);
        email.setCharset(charset);
        email.setSocketConnectionTimeout(connectTimeoutMs);
        email.setSocketTimeout(connectTimeoutMs);
    }

    @Override
    public void close() {

    }

    @Override
    public int getWeight(BalancerAlgorithm balancerAlgorithm) {
        return 0;
    }

}
