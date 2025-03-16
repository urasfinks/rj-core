package ru.jamsys.core.resource.notification.android;

import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import ru.jamsys.core.extension.property.repository.AnnotationPropertyExtractor;
import ru.jamsys.core.extension.annotation.PropertyKey;

@SuppressWarnings({"UnusedDeclaration"})
@FieldNameConstants
@Getter
public class AndroidNotificationProperty extends AnnotationPropertyExtractor<Object> {

    @PropertyKey("notification.android.url")
    private String url;

    @PropertyKey("notification.android.application.name")
    private String applicationName;

    @PropertyKey("notification.android.timeoutMs")
    private Integer timeoutMs;

    @PropertyKey("notification.android.messaging.scope")
    private String scope;

    @PropertyKey("notification.android.storage.credentials")
    private String storageCredentials;

}
