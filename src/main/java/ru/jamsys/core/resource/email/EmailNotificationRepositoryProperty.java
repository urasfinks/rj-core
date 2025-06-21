package ru.jamsys.core.resource.email;

import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import ru.jamsys.core.extension.annotation.PropertyKey;
import ru.jamsys.core.extension.property.repository.RepositoryPropertyAnnotationField;

@SuppressWarnings({"UnusedDeclaration"})
@FieldNameConstants
@Getter
public class EmailNotificationRepositoryProperty extends RepositoryPropertyAnnotationField<Object> {

    @PropertyKey("host")
    private String host;

    @PropertyKey("user")
    private String user;

    @PropertyKey("from")
    private String from;

    @PropertyKey("charset")
    private String charset;

    @PropertyKey("security.alias")
    private String securityAlias;

    @PropertyKey("port")
    private Integer port;

    @PropertyKey("timeoutMs")
    private Integer connectTimeoutMs;

    @PropertyKey("ssl")
    private Boolean ssl;

    @PropertyKey("support.address")
    private String support;

    @PropertyKey("template.path")
    private String templatePath;

    @PropertyKey("template.file.loader")
    private String templateFileLoader;

}
