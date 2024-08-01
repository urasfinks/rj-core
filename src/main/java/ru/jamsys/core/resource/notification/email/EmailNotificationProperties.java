package ru.jamsys.core.resource.notification.email;

import lombok.Getter;
import ru.jamsys.core.extension.property.repository.RepositoryPropertiesField;
import ru.jamsys.core.extension.annotation.PropertyName;

@SuppressWarnings({"UnusedDeclaration"})
@Getter
public class EmailNotificationProperties extends RepositoryPropertiesField {

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
