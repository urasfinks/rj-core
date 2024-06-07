package ru.jamsys.core.resource.notification.android;

import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ExceptionHandler;
import ru.jamsys.core.component.PropComponent;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.resource.NamespaceResourceConstructor;
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
        implements Resource<NamespaceResourceConstructor, AndroidNotificationRequest, HttpResponse> {

    private String url;

    private String applicationName;

    private int timeoutMs;

    private String accessToken;

    private String scope;

    private String storageCredentials;

    @Override
    public void constructor(NamespaceResourceConstructor constructor) throws Throwable {
        PropComponent propComponent = App.context.getBean(PropComponent.class);

        propComponent.getProp(constructor.ns, "notification.android.url", s -> this.url = s);
        propComponent.getProp(constructor.ns, "notification.android.application.name", s -> this.applicationName = s);
        propComponent.getProp(constructor.ns, "notification.android.timeoutMs", s -> this.timeoutMs = Integer.parseInt(s));

        propComponent.getProp(constructor.ns, "notification.android.messaging.scope", s -> {
            this.scope = s;
            reInitClient();
        });

        propComponent.getProp(constructor.ns, "notification.android.storage.credentials", s -> {
            this.storageCredentials = s;
            reInitClient();
        });
    }

    private void reInitClient() {
        if (scope == null || storageCredentials == null) {
            return;
        }
        try {
            String[] messagingScope = new String[]{scope};
            GoogleCredentials googleCredentials = GoogleCredentials
                    .fromStream(new FileInputStream(storageCredentials))
                    .createScoped(Arrays.asList(messagingScope));
            googleCredentials.refresh();
            this.accessToken = googleCredentials.getAccessToken().getTokenValue();
        } catch (Exception e) {
            App.context.getBean(ExceptionHandler.class).handler(e);
        }
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
