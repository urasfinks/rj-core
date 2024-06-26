package ru.jamsys.core.resource.notification.email;

import lombok.Getter;
import ru.jamsys.core.extension.property.PropertyConnector;
import ru.jamsys.core.extension.property.PropertyName;

@SuppressWarnings({"UnusedDeclaration"})
@Getter
public class EmailNotificationProperty extends PropertyConnector {

    @PropertyName("notification.email.host")
    private String host;

    @PropertyName("notification.email.user")
    private String user;

    @PropertyName("notification.email.from")
    private String from;

    @PropertyName("notification.email.charset")
    private String charset;

    @PropertyName("notification.email.security.alias")
    private String securityAlias;

    @PropertyName("notification.email.port")
    private String port;

    @PropertyName("notification.email.timeoutMs")
    private String connectTimeoutMs;

    @PropertyName("notification.email.ssl")
    private String ssl;

}
