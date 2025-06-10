package ru.jamsys.core.resource.notification.telegram;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;
import ru.jamsys.core.extension.annotation.PropertyKey;
import ru.jamsys.core.extension.annotation.PropertyNotNull;
import ru.jamsys.core.extension.property.repository.RepositoryPropertyAnnotationField;

@SuppressWarnings({"UnusedDeclaration"})
@FieldNameConstants
@Getter
@Setter
@Accessors(chain = true)
public class TelegramNotificationRepositoryProperty extends RepositoryPropertyAnnotationField<Object> {

    @PropertyNotNull
    @PropertyKey("security.alias")
    private String securityAlias;

    @PropertyKey("idChat")
    private String idChat;

    @PropertyKey("message")
    private String message;

}
