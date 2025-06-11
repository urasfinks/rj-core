package ru.jamsys.core.plugin.http.resource.notification.android;

import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import ru.jamsys.core.extension.property.repository.RepositoryPropertyAnnotationField;
import ru.jamsys.core.extension.annotation.PropertyKey;

import java.util.Map;

@SuppressWarnings({"UnusedDeclaration", "all"})
@FieldNameConstants
@Getter
public class AndroidNotificationRepositoryProperty extends RepositoryPropertyAnnotationField<Object> {

    @PropertyKey("application.name")
    private String applicationName;

    @PropertyKey("title")
    private String title;

    @PropertyKey("data")
    private Map<String, Object> data;

    @PropertyKey("token")
    private String token;

}
