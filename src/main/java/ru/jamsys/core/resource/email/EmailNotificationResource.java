package ru.jamsys.core.resource.email;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.HtmlEmail;
import ru.jamsys.core.App;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.expiration.AbstractExpirationResource;
import ru.jamsys.core.extension.property.PropertyDispatcher;

public class EmailNotificationResource extends AbstractExpirationResource {

    private final SecurityComponent securityComponent;

    private final PropertyDispatcher<Object> propertyDispatcher;

    @Getter
    private final EmailNotificationRepositoryProperty property = new EmailNotificationRepositoryProperty();

    private final String ns;

    public EmailNotificationResource(String ns) {
        securityComponent = App.get(SecurityComponent.class);
        propertyDispatcher = new PropertyDispatcher<>(
                null,
                property,
                getCascadeKey(ns)
        );
        this.ns = ns;
    }

    public Void execute(EmailNotificationRequest arguments) {
        HtmlEmail email = new HtmlEmail();
        try {

            email.setHostName(property.getHost());
            email.setSmtpPort(property.getPort());
            email.setAuthenticator(new DefaultAuthenticator(
                    property.getUser(),
                    new String(securityComponent.get(property.getSecurityAlias())))
            );
            email.setSSLOnConnect(property.getSsl());
            email.setFrom(property.getFrom());
            email.setCharset(property.getCharset());
            email.setSocketConnectionTimeout(property.getConnectTimeoutMs());
            email.setSocketTimeout(property.getConnectTimeoutMs());

            email.addTo(arguments.getTo());
            email.setSubject(arguments.getTitle());
            if (arguments.getData() != null) {
                email.setTextMsg(arguments.getData());
            }
            email.setHtmlMsg(arguments.getDataHtml());
            email.send();
        } catch (Exception e) {
            throw new ForwardException(this, e);
        }
        return null;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void runOperation() {
        propertyDispatcher.run();
    }

    @Override
    public void shutdownOperation() {
        propertyDispatcher.shutdown();
    }

    @Override
    public boolean checkFatalException(Throwable th) {
        return false;
    }

    @JsonValue
    public Object getJsonValue() {
        return new HashMapBuilder<>()
                .append("hashCode", Integer.toHexString(hashCode()))
                .append("cls", getClass())
                .append("ns", ns)
                .append("propertyDispatcherNs", propertyDispatcher.getNs())
                ;
    }

}
