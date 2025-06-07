package ru.jamsys.core.resource.http;

import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import ru.jamsys.core.extension.annotation.PropertyDescription;
import ru.jamsys.core.extension.annotation.PropertyKey;
import ru.jamsys.core.extension.annotation.PropertyNotNull;
import ru.jamsys.core.extension.annotation.PropertyValueRegexp;
import ru.jamsys.core.extension.property.repository.RepositoryPropertyAnnotationField;

@SuppressWarnings({"UnusedDeclaration"})
@FieldNameConstants
@Getter
public class HttpResourceRepositoryProperty extends RepositoryPropertyAnnotationField<Object> {

    @SuppressWarnings("all")
    @PropertyNotNull
    @PropertyKey("type")
    @PropertyDescription("Тип коннектора Default/Apache")
    private volatile String type = "Default";

    @PropertyKey("url")
    @PropertyDescription("Url")
    private volatile String url;

    @PropertyKey("method")
    @PropertyDescription("GET/POST/PUT/DELETE")
    @PropertyValueRegexp("^(GET|POST|PUT|DELETE|HEAD|OPTIONS|PATCH|TRACE|CONNECT)$")
    private volatile String method;

    @PropertyKey("header")
    @PropertyDescription("Заголовки в формате GET key=value&key2=value2")
    private volatile String header;

}
