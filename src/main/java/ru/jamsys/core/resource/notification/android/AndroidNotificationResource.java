package ru.jamsys.core.resource.notification.android;

import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.extension.CascadeKey;
import ru.jamsys.core.extension.expiration.AbstractExpirationResource;
import ru.jamsys.core.extension.log.DataHeader;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.extension.property.PropertyListener;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.resource.http.client.HttpConnector;
import ru.jamsys.core.resource.http.client.HttpConnectorDefault;
import ru.jamsys.core.resource.http.client.HttpResponse;

import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class AndroidNotificationResource extends AbstractExpirationResource implements PropertyListener {

    private String accessToken;

    private final PropertyDispatcher<Object> propertyDispatcher;

    private final AndroidNotificationRepositoryProperty androidNotificationRepositoryProperty = new AndroidNotificationRepositoryProperty();

    public AndroidNotificationResource(String ns) {
        propertyDispatcher = new PropertyDispatcher<>(
                this,
                androidNotificationRepositoryProperty,
                getCascadeKey(ns)
        );
    }

    public HttpResponse execute(AndroidNotificationRequest arguments) {
        String postData = createPostData(arguments.getTitle(), arguments.getData(), arguments.getToken());
        HttpConnector httpConnector = new HttpConnectorDefault()
                .setUrl(androidNotificationRepositoryProperty.getUrl())
                .setConnectTimeoutMs(1_000)
                .setReadTimeoutMs(androidNotificationRepositoryProperty.getTimeoutMs())
                .setRequestHeader("Content-type", "application/json")
                .setRequestHeader("Authorization", "Bearer " + accessToken)
                .setPostData(postData.getBytes(StandardCharsets.UTF_8));
        httpConnector.exec();
        return httpConnector.getResponseObject();
    }

    @Override
    public boolean isValid() {
        return true;
    }

    private String createPostData(String title, Map<String, Object> data, String token) {
        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> message = new LinkedHashMap<>();
        Map<String, Object> notification = new LinkedHashMap<>();

        message.put("token", token);

        notification.put("title", androidNotificationRepositoryProperty.getApplicationName());
        notification.put("body", title);
        message.put("notification", notification);

        if (data != null) {
            Map<String, Object> dataObject = new LinkedHashMap<>();
            dataObject.put("message", UtilJson.toString(data, "{}"));
            message.put("data", dataObject);
        }

        root.put("message", message);

        return UtilJson.toStringPretty(root, "{}");
    }

    @Override
    public void runOperation() {
        if (propertyDispatcher != null) {
            propertyDispatcher.run();
        }
    }

    @Override
    public void shutdownOperation() {
        if (propertyDispatcher != null) {
            propertyDispatcher.shutdown();
        }
    }

    @Override
    public void onPropertyUpdate(String key, String oldValue, String newValue) {
        if (androidNotificationRepositoryProperty.getScope() == null || androidNotificationRepositoryProperty.getStorageCredentials() == null) {
            return;
        }
        try {
            String[] messagingScope = new String[]{androidNotificationRepositoryProperty.getScope()};
            GoogleCredentials googleCredentials = GoogleCredentials
                    .fromStream(new FileInputStream(androidNotificationRepositoryProperty.getStorageCredentials()))
                    .createScoped(Arrays.asList(messagingScope));
            googleCredentials.refresh();
            this.accessToken = googleCredentials.getAccessToken().getTokenValue();
        } catch (Exception e) {
            App.error(e);
        }
    }

    public String getNs() {
        return App.getUniqueClassName(getClass());
    }

    @Override
    public CascadeKey getParentCascadeKey() {
        return null;
    }

    @Override
    public boolean checkFatalException(Throwable th) {
        return false;
    }

    @Override
    public List<DataHeader> flushAndGetStatistic(AtomicBoolean threadRun) {
        return List.of();
    }

}
