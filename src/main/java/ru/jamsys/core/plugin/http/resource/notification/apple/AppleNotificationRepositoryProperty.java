package ru.jamsys.core.plugin.http.resource.notification.apple;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;
import ru.jamsys.core.extension.annotation.PropertyDescription;
import ru.jamsys.core.extension.annotation.PropertyNotNull;
import ru.jamsys.core.extension.property.repository.RepositoryPropertyAnnotationField;
import ru.jamsys.core.extension.annotation.PropertyKey;

import java.util.Map;

@SuppressWarnings({"UnusedDeclaration", "all"})
@FieldNameConstants
@Getter
@Setter
@Accessors(chain = true)
public class AppleNotificationRepositoryProperty extends RepositoryPropertyAnnotationField<Object> {

    @PropertyNotNull
    @PropertyKey("virtual.path")
    private String virtualPath;

    @PropertyNotNull
    @PropertyKey("security.alias")
    private String securityAlias;

    @PropertyNotNull
    @PropertyKey("topic")
    private String topic;

    @PropertyNotNull
    @PropertyKey("priority")
    private String priority;

    @PropertyNotNull
    @PropertyKey("expiration")
    private String expiration;

    @PropertyNotNull
    @PropertyKey("pushType")
    private String pushType;

    @PropertyNotNull
    @PropertyKey("path.storage")
    private String pathStorage;

    @PropertyNotNull
    @PropertyKey("device")
    @PropertyDescription("Уникальный код девайса")
    private String device;

    @PropertyNotNull
    @PropertyKey("title")
    @PropertyDescription("Заголовок сообщения")
    private String title;

    @PropertyNotNull
    @PropertyKey("payload")
    @PropertyDescription("Полезная нагрузка")
    private Map<String, Object> payload;

}
