package ru.jamsys.core.resource.jdbc;

import lombok.Getter;
import ru.jamsys.core.extension.property.repository.AnnotationPropertyExtractor;
import ru.jamsys.core.extension.annotation.PropertyName;

@SuppressWarnings({"UnusedDeclaration"})
@Getter
public class JdbcProperty extends AnnotationPropertyExtractor {

    @PropertyName("jdbc.uri")
    private String uri;

    @PropertyName("jdbc.user")
    private String user;

    @PropertyName("jdbc.security.alias")
    private String securityAlias;

}
