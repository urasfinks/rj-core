package ru.jamsys.core.resource.jdbc;

import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import ru.jamsys.core.extension.property.repository.RepositoryPropertyAnnotationField;
import ru.jamsys.core.extension.annotation.PropertyKey;

@SuppressWarnings({"UnusedDeclaration"})
@FieldNameConstants
@Getter
public class JdbcRepositoryProperty extends RepositoryPropertyAnnotationField<String> {

    @PropertyKey("jdbc.uri")
    private String uri;

    @PropertyKey("jdbc.user")
    private String user;

    @PropertyKey("jdbc.security.alias")
    private String securityAlias;

}
