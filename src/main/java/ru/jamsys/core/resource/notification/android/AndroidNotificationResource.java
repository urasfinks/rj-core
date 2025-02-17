package ru.jamsys.core.resource.notification.android;

import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.property.PropertiesAgent;
import ru.jamsys.core.extension.property.PropertyUpdateDelegate;
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
        PropertyUpdateDelegate {

    private String accessToken;

    private PropertiesAgent propertiesAgent;

    private final AndroidNotificationProperties property = new AndroidNotificationProperties();

    @Override
    public void setArguments(ResourceArguments resourceArguments) throws Throwable {
        ServiceProperty serviceProperty = App.get(ServiceProperty.class);
        propertiesAgent = serviceProperty.getFactory().getPropertiesAgent(
                this,
                property,
                resourceArguments.ns,
                true
        );
    }

    @Override
    public void onPropertyUpdate(Map<String, String> mapAlias) {
        if (property.getScope() == null || property.getStorageCredentials() == null) {
            return;
        }
        try {
            String[] messagingScope = new String[]{property.getScope()};
            GoogleCredentials googleCredentials = GoogleCredentials
                    .fromStream(new FileInputStream(property.getStorageCredentials()))
                    .createScoped(Arrays.asList(messagingScope));
            googleCredentials.refresh();
            this.accessToken = googleCredentials.getAccessToken().getTokenValue();
        } catch (Exception e) {
            App.error(e);
        }
    }

    @Override
    public HttpResponse execute(AndroidNotificationRequest arguments) {
        String postData = createPostData(arguments.getTitle(), arguments.getData(), arguments.getToken());
        HttpConnector httpConnector = new HttpConnectorDefault()
                .setUrl(property.getUrl())
                .setTimeoutMs(property.getTimeoutMs())
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

        notification.put("title", property.getApplicationName());
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
    public void run() {
        if (propertiesAgent != null) {
            propertiesAgent.run();
        }
    }

    @Override
    public void shutdown() {
        if (propertiesAgent != null) {
            propertiesAgent.shutdown();
        }
    }

    @Override
    public Function<Throwable, Boolean> getFatalException() {
        return _ -> false;
    }

}
