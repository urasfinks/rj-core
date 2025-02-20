package ru.jamsys.core.resource.notification.email;

import lombok.Getter;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.HtmlEmail;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.property.PropertySubscriber;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.resource.ResourceArguments;
import ru.jamsys.core.resource.ResourceCheckException;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;

import java.util.function.Function;

@Component
@Scope("prototype")
public class EmailNotificationResource
        extends ExpirationMsMutableImpl
        implements
        Resource<EmailNotificationRequest, Void>,
        ResourceCheckException {

    private SecurityComponent securityComponent;

    private PropertySubscriber propertySubscriber;

    @Getter
    private final EmailNotificationProperty property = new EmailNotificationProperty();

    @Override
    public void setArguments(ResourceArguments resourceArguments) throws Throwable {
        securityComponent = App.get(SecurityComponent.class);
        propertySubscriber = new PropertySubscriber(
                App.get(ServiceProperty.class),
                null,
                property,
                resourceArguments.ns
        );
    }

    @Override
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
            throw new ForwardException(e);
        }
        return null;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Function<Throwable, Boolean> getFatalException() {
        return _ -> false;
    }

    @Override
    public void run() {
        propertySubscriber.run();

    }

    @Override
    public void shutdown() {
        propertySubscriber.shutdown();

    }

}
