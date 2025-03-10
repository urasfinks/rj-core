package ru.jamsys.core.resource.notification.apple;

import lombok.Getter;
import ru.jamsys.core.extension.property.repository.AnnotationPropertyExtractor;
import ru.jamsys.core.extension.annotation.PropertyName;

@SuppressWarnings({"UnusedDeclaration"})
@Getter
public class AppleNotificationProperty extends AnnotationPropertyExtractor {

    @PropertyName("notification.apple.virtual.path")
    private String virtualPath;

    @PropertyName("notification.apple.security.alias")
    private String securityAlias;

    @PropertyName("notification.apple.url")
    private String url;

    @PropertyName("notification.apple.topic")
    private String topic;

    @PropertyName("notification.apple.priority")
    private String priority;

    @PropertyName("notification.apple.expiration")
    private String expiration;

    @PropertyName("notification.apple.pushType")
    private String pushType;


    @PropertyName("notification.apple.timeoutMs")
    private Integer timeoutMs;

    @PropertyName("notification.apple.storage")
    private String storage;

}
