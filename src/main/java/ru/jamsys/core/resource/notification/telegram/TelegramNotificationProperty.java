package ru.jamsys.core.resource.notification.telegram;

import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import ru.jamsys.core.extension.property.repository.AnnotationPropertyExtractor;
import ru.jamsys.core.extension.annotation.PropertyKey;

@SuppressWarnings({"UnusedDeclaration"})
@FieldNameConstants
@Getter
public class TelegramNotificationProperty extends AnnotationPropertyExtractor<Object> {

    @PropertyKey("notification.telegram.security.alias")
    private String securityAlias;

    @PropertyKey("notification.telegram.url")
    private String url;

    @PropertyKey("notification.telegram.idChat")
    private String idChat;

    @PropertyKey("notification.telegram.timeoutMs")
    private Integer timeoutMs;

}
