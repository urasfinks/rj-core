package ru.jamsys.core.resource.jdbc;

import lombok.Getter;
import ru.jamsys.core.extension.PropertyConnector;
import ru.jamsys.core.extension.PropertyName;

@SuppressWarnings({"UnusedDeclaration"})
@Getter
public class JdbcProperty extends PropertyConnector {

    @PropertyName("jdbc.uri")
    private String uri;

    @PropertyName("jdbc.user")
    private String user;

    @PropertyName("jdbc.security.alias")
    private String securityAlias;

}
