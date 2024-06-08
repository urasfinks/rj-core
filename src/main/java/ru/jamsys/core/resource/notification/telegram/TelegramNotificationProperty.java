package ru.jamsys.core.resource.notification.telegram;

import lombok.Getter;
import ru.jamsys.core.extension.PropertyConnector;
import ru.jamsys.core.extension.PropertyName;

@SuppressWarnings({"UnusedDeclaration"})
@Getter
public class TelegramNotificationProperty extends PropertyConnector {

    @PropertyName("notification.telegram.security.alias")
    private String securityAlias;

    @PropertyName("notification.telegram.url")
    private String url;

    @PropertyName("notification.telegram.idChat")
    private String idChat;

    @PropertyName("notification.telegram.timeoutMs")
    private String timeoutMs;

}
