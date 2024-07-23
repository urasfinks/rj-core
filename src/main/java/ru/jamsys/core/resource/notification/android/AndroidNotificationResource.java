package ru.jamsys.core.resource.notification.android;

import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.property.PropertyUpdateDelegate;
import ru.jamsys.core.extension.property.PropertiesNsAgent;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.resource.ResourceArguments;
import ru.jamsys.core.resource.http.client.HttpClient;
import ru.jamsys.core.resource.http.client.HttpClientImpl;
import ru.jamsys.core.resource.http.client.HttpResponse;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;

import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Component
public class AndroidNotificationResource
        extends ExpirationMsMutableImpl
        implements
        Resource<AndroidNotificationRequest, HttpResponse>,
        PropertyUpdateDelegate {

    private String accessToken;

    private PropertiesNsAgent propertiesNsAgent;

    private final AndroidNotificationProperties property = new AndroidNotificationProperties();

    @Override
    public void setArguments(ResourceArguments resourceArguments) throws Throwable {
        ServiceProperty serviceProperty = App.get(ServiceProperty.class);
        propertiesNsAgent = serviceProperty.getFactory().getNsAgent(this, property, resourceArguments.ns);
    }

    @Override
    public void onPropertyUpdate(Set<String> updatedPropAlias) {
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
        HttpClient httpClient = new HttpClientImpl();
        httpClient.setUrl(property.getUrl());
        httpClient.setTimeoutMs(Integer.parseInt(property.getTimeoutMs()));
        httpClient.setRequestHeader("Content-type", "application/json");
        httpClient.setRequestHeader("Authorization", "Bearer " + accessToken);
        String postData = createPostData(arguments.getTitle(), arguments.getData(), arguments.getToken());

        httpClient.setPostData(postData.getBytes(StandardCharsets.UTF_8));
        httpClient.exec();

        return httpClient.getHttpResponseEnvelope();

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
        if (propertiesNsAgent != null) {
            propertiesNsAgent.run();
        }
    }

    @Override
    public void shutdown() {
        if (propertiesNsAgent != null) {
            propertiesNsAgent.shutdown();
        }
    }

    @Override
    public Function<Throwable, Boolean> getFatalException() {
        return _ -> false;
    }

}
