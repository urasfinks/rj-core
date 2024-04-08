package ru.jamsys.notification;

import com.google.auth.oauth2.GoogleCredentials;
import lombok.Setter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.App;
import ru.jamsys.component.PropertiesManager;
import ru.jamsys.http.HttpClient;
import ru.jamsys.http.HttpClientNewImpl;
import ru.jamsys.http.JsonHttpResponse;
import ru.jamsys.util.UtilJson;

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

    public NotificationAndroid(PropertiesManager propertiesManager) {
        this.url = propertiesManager.getProperties("rj.notification.android.url", String.class);
        this.messagingScope = new String[]{propertiesManager.getProperties("rj.notification.android.messaging.scope", String.class)};
        this.storageCredentials = propertiesManager.getProperties("rj.notification.android.storage.credentials", String.class);
        this.applicationName = propertiesManager.getProperties("rj.notification.android.application.name", String.class);
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
    public JsonHttpResponse notify(String title, Object data, String token) {
        JsonHttpResponse jRet = new JsonHttpResponse();

        if (jRet.isStatus() && token == null) {
            jRet.addException("token is null");
        }

        HttpClient httpClient = null;
        String postData = null;

        if (jRet.isStatus()) {
            httpClient = new HttpClientNewImpl();
            httpClient.setUrl(url);
            httpClient.setRequestHeader("Content-type", "application/json");
            try {
                httpClient.setRequestHeader("Authorization", "Bearer " + getAccessToken());
            } catch (Exception e) {
                jRet.addException(e);
            }
        }

        if (jRet.isStatus()) {
            postData = createPostData(title, data, token);
            if (postData == null || postData.trim().equals("")) {
                jRet.addException("postData is empty");
            }
        }
        if (jRet.isStatus() && httpClient != null && postData != null) {
            httpClient.setPostData(postData.getBytes(StandardCharsets.UTF_8));
            httpClient.exec();
        }
        if (jRet.isStatus() && httpClient != null && httpClient.getException() != null) {
            jRet.addException(httpClient.getException());
        }
        return jRet;
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
        PropertiesManager propertiesManager = App.context.getBean(PropertiesManager.class);
        return new NotificationAndroid(propertiesManager);
    }

}
