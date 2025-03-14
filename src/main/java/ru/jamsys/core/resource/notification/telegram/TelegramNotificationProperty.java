package ru.jamsys.core.resource.notification.telegram;

import lombok.Getter;
import ru.jamsys.core.extension.property.repository.AnnotationPropertyExtractor;
import ru.jamsys.core.extension.annotation.PropertyName;

@SuppressWarnings({"UnusedDeclaration"})
@Getter
public class TelegramNotificationProperty extends AnnotationPropertyExtractor {

    @PropertyName("notification.telegram.security.alias")
    private String securityAlias;

    @PropertyName("notification.telegram.url")
    private String url;

    @PropertyName("notification.telegram.idChat")
    private String idChat;

    @PropertyName("notification.telegram.timeoutMs")
    private Integer timeoutMs;

}
