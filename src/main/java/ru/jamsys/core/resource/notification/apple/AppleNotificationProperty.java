package ru.jamsys.core.resource.notification.apple;

import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import ru.jamsys.core.extension.property.repository.AnnotationPropertyExtractor;
import ru.jamsys.core.extension.annotation.PropertyKey;

@SuppressWarnings({"UnusedDeclaration"})
@FieldNameConstants
@Getter
public class AppleNotificationProperty extends AnnotationPropertyExtractor<Object> {

    @PropertyKey("notification.apple.virtual.path")
    private String virtualPath;

    @PropertyKey("notification.apple.security.alias")
    private String securityAlias;

    @PropertyKey("notification.apple.url")
    private String url;

    @PropertyKey("notification.apple.topic")
    private String topic;

    @PropertyKey("notification.apple.priority")
    private String priority;

    @PropertyKey("notification.apple.expiration")
    private String expiration;

    @PropertyKey("notification.apple.pushType")
    private String pushType;


    @PropertyKey("notification.apple.timeoutMs")
    private Integer timeoutMs;

    @PropertyKey("notification.apple.storage")
    private String storage;

}
