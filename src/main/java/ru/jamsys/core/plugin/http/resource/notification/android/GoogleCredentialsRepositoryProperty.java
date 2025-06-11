package ru.jamsys.core.plugin.http.resource.notification.android;

import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import ru.jamsys.core.extension.annotation.PropertyKey;
import ru.jamsys.core.extension.annotation.PropertyNotNull;
import ru.jamsys.core.extension.property.repository.RepositoryPropertyAnnotationField;

@SuppressWarnings({"UnusedDeclaration", "all"})
@FieldNameConstants
@Getter
public class GoogleCredentialsRepositoryProperty extends RepositoryPropertyAnnotationField<String> {

    @PropertyNotNull
    @PropertyKey("messaging.scope")
    private String scope;

    @PropertyNotNull
    @PropertyKey("storage.credentials")
    private String storageCredentials;

}
