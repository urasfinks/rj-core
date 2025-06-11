package ru.jamsys.core.plugin.http.resource.notification.telegram;

import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import ru.jamsys.core.extension.annotation.PropertyKey;
import ru.jamsys.core.extension.annotation.PropertyNotNull;
import ru.jamsys.core.extension.property.repository.RepositoryPropertyAnnotationField;

@SuppressWarnings({"UnusedDeclaration"})
@FieldNameConstants
@Getter
public class TelegramNotificationRepositoryProperty extends RepositoryPropertyAnnotationField<Object> {

    @PropertyNotNull
    @PropertyKey("security.alias")
    private String securityAlias;

    @PropertyNotNull
    @PropertyKey("idChat")
    private String idChat;

    @PropertyNotNull
    @PropertyKey("message")
    private String message;

}
