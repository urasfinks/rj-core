package ru.jamsys.core.resource.notification.android;

import lombok.Getter;
import ru.jamsys.core.extension.property.repository.RepositoryPropertiesField;
import ru.jamsys.core.extension.annotation.PropertyName;

@SuppressWarnings({"UnusedDeclaration"})
@Getter
public class AndroidNotificationProperties extends RepositoryPropertiesField {

    @PropertyName("notification.android.url")
    private String url;

    @PropertyName("notification.android.application.name")
    private String applicationName;

    @PropertyName("notification.android.timeoutMs")
    private Integer timeoutMs;

    @PropertyName("notification.android.messaging.scope")
    private String scope;

    @PropertyName("notification.android.storage.credentials")
    private String storageCredentials;

}
