package ru.jamsys.core.resource.notification.android;

import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.PropertiesComponent;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.resource.balancer.algorithm.BalancerAlgorithm;
import ru.jamsys.core.resource.http.client.HttpClient;
import ru.jamsys.core.resource.http.client.HttpClientImpl;
import ru.jamsys.core.resource.http.client.HttpResponse;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;

import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class AndroidNotificationResource
        extends ExpirationMsMutableImpl
        implements Resource<AndroidNotificationResourceConstructor, AndroidNotificationRequest, HttpResponse> {

    private String url;

    private String applicationName;

    private int timeoutMs;

    private String accessToken;

    @Override
    public void constructor(AndroidNotificationResourceConstructor constructor) throws Throwable {
        PropertiesComponent propertiesComponent = App.context.getBean(PropertiesComponent.class);

        this.url = propertiesComponent.getProperties(constructor.namespaceProperties, "notification.android.url", String.class);
        this.applicationName = propertiesComponent.getProperties(constructor.namespaceProperties, "notification.android.application.name", String.class);
        this.timeoutMs = propertiesComponent.getProperties(constructor.namespaceProperties, "notification.android.timeoutMs", Integer.class);

        String[] messagingScope = new String[]{propertiesComponent.getProperties(constructor.namespaceProperties, "notification.android.messaging.scope", String.class)};
        String storageCredentials = propertiesComponent.getProperties(constructor.namespaceProperties, "notification.android.storage.credentials", String.class);

        GoogleCredentials googleCredentials = GoogleCredentials
                .fromStream(new FileInputStream(storageCredentials))
                .createScoped(Arrays.asList(messagingScope));
        googleCredentials.refresh();
        this.accessToken = googleCredentials.getAccessToken().getTokenValue();
    }

    @Override
    public HttpResponse execute(AndroidNotificationRequest arguments) {
        HttpClient httpClient = new HttpClientImpl();
        httpClient.setUrl(url);
        httpClient.setTimeoutMs(timeoutMs);
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

        notification.put("title", applicationName);
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
    public void close() {

    }

    @Override
    public int getWeight(BalancerAlgorithm balancerAlgorithm) {
        return 0;
    }

}
