package ru.jamsys.core.resource.notification.email;

import lombok.Getter;
import ru.jamsys.core.extension.annotation.PropertyName;
import ru.jamsys.core.extension.property.repository.AnnotationPropertyExtractor;

@SuppressWarnings({"UnusedDeclaration"})
@Getter
public class EmailNotificationProperty extends AnnotationPropertyExtractor {

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
    private Integer port;

    @PropertyName("notification.email.timeoutMs")
    private Integer connectTimeoutMs;

    @PropertyName("notification.email.ssl")
    private Boolean ssl;

    @PropertyName("notification.email.support.address")
    private String support;

    @PropertyName("notification.email.template.path")
    private String templatePath;

    @PropertyName("notification.email.template.class.loader")
    private String templateClassLoader;

}
