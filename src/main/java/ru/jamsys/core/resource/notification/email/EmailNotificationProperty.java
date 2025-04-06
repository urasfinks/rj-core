package ru.jamsys.core.resource.notification.email;

import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import ru.jamsys.core.extension.annotation.PropertyKey;
import ru.jamsys.core.extension.property.repository.AnnotationPropertyExtractor;

@SuppressWarnings({"UnusedDeclaration"})
@FieldNameConstants
@Getter
public class EmailNotificationProperty extends AnnotationPropertyExtractor<Object> {

    @PropertyKey("notification.email.host")
    private String host;

    @PropertyKey("notification.email.user")
    private String user;

    @PropertyKey("notification.email.from")
    private String from;

    @PropertyKey("notification.email.charset")
    private String charset;

    @PropertyKey("notification.email.security.alias")
    private String securityAlias;

    @PropertyKey("notification.email.port")
    private Integer port;

    @PropertyKey("notification.email.timeoutMs")
    private Integer connectTimeoutMs;

    @PropertyKey("notification.email.ssl")
    private Boolean ssl;

    @PropertyKey("notification.email.support.address")
    private String support;

    @PropertyKey("notification.email.template.path")
    private String templatePath;

    @PropertyKey("notification.email.template.class.loader")
    private String templateClassLoader;

}
