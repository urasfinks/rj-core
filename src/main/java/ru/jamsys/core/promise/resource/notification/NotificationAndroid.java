package ru.jamsys.core.promise.resource.notification;

import com.google.auth.oauth2.GoogleCredentials;
import lombok.Setter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.PropertiesComponent;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.resource.http.client.HttpClient;
import ru.jamsys.core.resource.http.client.HttpClientImpl;
import ru.jamsys.core.resource.http.client.HttpResponseEnvelope;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Lazy
public class NotificationAndroid implements Notification {

    @Setter
    private String storageCredentials;

    @Setter
    private String url;

    @Setter
    private String applicationName;

    private final String[] messagingScope;

    @Setter
    private int timeoutMs;

    public NotificationAndroid(PropertiesComponent propertiesComponent) {
        this.url = propertiesComponent.getProperties("default.notification.android.url", String.class);
        this.messagingScope = new String[]{propertiesComponent.getProperties("default.notification.android.messaging.scope", String.class)};
        this.storageCredentials = propertiesComponent.getProperties("default.notification.android.storage.credentials", String.class);
        this.applicationName = propertiesComponent.getProperties("default.notification.android.application.name", String.class);
        this.timeoutMs = propertiesComponent.getProperties("default.notification.android.timeoutMs", Integer.class);
    }

    private String getAccessToken() throws IOException {
        //TODO: надо прихранивать токен, а то задосим гугл)
        GoogleCredentials googleCredentials = GoogleCredentials
                .fromStream(new FileInputStream(storageCredentials))
                .createScoped(Arrays.asList(messagingScope));
        googleCredentials.refresh();
        return googleCredentials.getAccessToken().getTokenValue();
    }

    @Override
    public HttpResponseEnvelope notify(String title, Object data, String token) {
        HttpResponseEnvelope httpResponseEnvelope = new HttpResponseEnvelope();

        if (httpResponseEnvelope.isStatus() && token == null) {
            httpResponseEnvelope.addException("token is null");
        }

        HttpClient httpClient = null;
        String postData = null;

        if (httpResponseEnvelope.isStatus()) {
            httpClient = new HttpClientImpl();
            httpClient.setUrl(url);
            httpClient.setTimeoutMs(timeoutMs);
            httpClient.setRequestHeader("Content-type", "application/json");

            try {
                httpClient.setRequestHeader("Authorization", "Bearer " + getAccessToken());
            } catch (Exception e) {
                httpResponseEnvelope.addException(e);
            }
        }

        if (httpResponseEnvelope.isStatus()) {
            postData = createPostData(title, data, token);
            if (postData == null || postData.trim().isEmpty()) {
                httpResponseEnvelope.addException("postData is empty");
            }
        }
        if (httpResponseEnvelope.isStatus() && postData != null) {
            httpClient.setPostData(postData.getBytes(StandardCharsets.UTF_8));
            httpClient.exec();
        }
        if (httpResponseEnvelope.isStatus()) {
            httpClient.getHttpResponseEnvelope();
        }
        return httpResponseEnvelope;
    }

    private String createPostData(String title, Object data, String token) {
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
    public Notification getInstance() {
        PropertiesComponent propertiesComponent = App.context.getBean(PropertiesComponent.class);
        return new NotificationAndroid(propertiesComponent);
    }

}
