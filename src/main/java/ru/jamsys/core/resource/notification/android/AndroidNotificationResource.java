package ru.jamsys.core.resource.notification.android;

import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.extension.property.PropertyListener;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.resource.ResourceArguments;
import ru.jamsys.core.resource.http.client.HttpConnector;
import ru.jamsys.core.resource.http.client.HttpConnectorDefault;
import ru.jamsys.core.resource.http.client.HttpResponse;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;

import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class AndroidNotificationResource
        extends ExpirationMsMutableImpl
        implements
        Resource<AndroidNotificationRequest, HttpResponse>,
        PropertyListener {

    private String accessToken;

    private PropertyDispatcher<Object> propertyDispatcher;

    private final AndroidNotificationProperty androidNotificationProperty = new AndroidNotificationProperty();

    @Override
    public void setArguments(ResourceArguments resourceArguments) throws Throwable {
        propertyDispatcher = new PropertyDispatcher<>(
                App.get(ServiceProperty.class),
                this,
                androidNotificationProperty,
                resourceArguments.ns
        );
    }

    @Override
    public HttpResponse execute(AndroidNotificationRequest arguments) {
        String postData = createPostData(arguments.getTitle(), arguments.getData(), arguments.getToken());
        HttpConnector httpConnector = new HttpConnectorDefault()
                .setUrl(androidNotificationProperty.getUrl())
                .setConnectTimeoutMs(1_000)
                .setReadTimeoutMs(androidNotificationProperty.getTimeoutMs())
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

        notification.put("title", androidNotificationProperty.getApplicationName());
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
    public boolean isRun() {
        if (propertyDispatcher != null) {
            return propertyDispatcher.isRun();
        }
        return false;
    }

    @Override
    public void run() {
        if (propertyDispatcher != null) {
            propertyDispatcher.run();
        }
    }

    @Override
    public void shutdown() {
        if (propertyDispatcher != null) {
            propertyDispatcher.shutdown();
        }
    }

    @Override
    public Function<Throwable, Boolean> getFatalException() {
        return _ -> false;
    }

    @Override
    public void onPropertyUpdate(String key, String oldValue, String newValue) {
        if (androidNotificationProperty.getScope() == null || androidNotificationProperty.getStorageCredentials() == null) {
            return;
        }
        try {
            String[] messagingScope = new String[]{androidNotificationProperty.getScope()};
            GoogleCredentials googleCredentials = GoogleCredentials
                    .fromStream(new FileInputStream(androidNotificationProperty.getStorageCredentials()))
                    .createScoped(Arrays.asList(messagingScope));
            googleCredentials.refresh();
            this.accessToken = googleCredentials.getAccessToken().getTokenValue();
        } catch (Exception e) {
            App.error(e);
        }
    }
}
