package ru.jamsys.core.resource.notification.email;

import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.HtmlEmail;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.extension.property.Subscriber;
import ru.jamsys.core.resource.NamespaceResourceConstructor;
import ru.jamsys.core.resource.ResourceCheckException;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.resource.balancer.algorithm.BalancerAlgorithm;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;

import java.util.function.Function;

@Component
@Scope("prototype")
public class EmailNotificationResource
        extends ExpirationMsMutableImpl
        implements
        Resource<NamespaceResourceConstructor, EmailNotificationRequest, Void>,
        ResourceCheckException
{

    private SecurityComponent securityComponent;

    private Subscriber subscriber;

    private final EmailNotificationProperty property = new EmailNotificationProperty();

    @Override
    public void constructor(NamespaceResourceConstructor constructor) throws Throwable {
        ServiceProperty serviceProperty = App.get(ServiceProperty.class);
        securityComponent = App.get(SecurityComponent.class);
        subscriber = serviceProperty.getSubscriber(null, property, constructor.ns);
    }

    @Override
    public Void execute(EmailNotificationRequest arguments) {
        HtmlEmail email = new HtmlEmail();
        try {

            email.setHostName(property.getHost());
            email.setSmtpPort(Integer.parseInt(property.getPort()));
            email.setAuthenticator(new DefaultAuthenticator(
                    property.getUser(),
                    new String(securityComponent.get(property.getSecurityAlias())))
            );
            email.setSSLOnConnect(Boolean.parseBoolean(property.getSsl()));
            email.setFrom(property.getFrom());
            email.setCharset(property.getCharset());
            email.setSocketConnectionTimeout(Integer.parseInt(property.getConnectTimeoutMs()));
            email.setSocketTimeout(Integer.parseInt(property.getConnectTimeoutMs()));

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

    @Override
    public void close() {
        if (subscriber != null) {
            subscriber.unsubscribe();
        }
    }

    @Override
    public int getWeight(BalancerAlgorithm balancerAlgorithm) {
        return 0;
    }

    @Override
    public Function<Throwable, Boolean> getFatalException() {
        return _ -> false;
    }

}
