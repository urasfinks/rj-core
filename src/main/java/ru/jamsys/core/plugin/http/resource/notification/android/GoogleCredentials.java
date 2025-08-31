package ru.jamsys.core.plugin.http.resource.notification.android;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import ru.jamsys.core.App;
import ru.jamsys.core.extension.AbstractManagerElement;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.extension.property.PropertyListener;

import java.io.FileInputStream;
import java.util.Arrays;

@Getter
public class GoogleCredentials extends AbstractManagerElement implements PropertyListener {

    private String accessToken;

    private final PropertyDispatcher<String> propertyDispatcher;

    private final GoogleCredentialsRepositoryProperty property = new GoogleCredentialsRepositoryProperty();

    private final String ns;

    private final String key;

    public GoogleCredentials(String ns, String key) {
        this.ns = ns;
        this.key = key;
        propertyDispatcher = new PropertyDispatcher<>(
                this,
                property,
                getCascadeKey(ns)
        );
    }

    @JsonValue
    public Object getJsonValue() {
        return new HashMapBuilder<String, Object>()
                .append("hashCode", Integer.toHexString(hashCode()))
                .append("cls", getClass())
                .append("ns", ns)
                .append("propertyDispatcherNs", propertyDispatcher.getNs())
                ;
    }

    @Override
    public void onPropertyUpdate(String key, String oldValue, String newValue) {
        if (property.getScope() == null || property.getStorageCredentials() == null) {
            return;
        }
        try {
            String[] messagingScope = new String[]{property.getScope()};
            com.google.auth.oauth2.GoogleCredentials googleCredentials = com.google.auth.oauth2.GoogleCredentials
                    .fromStream(new FileInputStream(property.getStorageCredentials()))
                    .createScoped(Arrays.asList(messagingScope));
            googleCredentials.refresh();
            this.accessToken = googleCredentials.getAccessToken().getTokenValue();
        } catch (Exception e) {
            App.error(e);
        }
    }

    @Override
    public void runOperation() {
        propertyDispatcher.run();
    }

    @Override
    public void shutdownOperation() {
        propertyDispatcher.shutdown();
    }

}
