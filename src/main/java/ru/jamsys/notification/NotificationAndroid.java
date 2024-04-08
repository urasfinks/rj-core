package ru.jamsys.notification;

import com.google.auth.oauth2.GoogleCredentials;
import lombok.Setter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.App;
import ru.jamsys.component.PropertiesManager;
import ru.jamsys.component.Security;
import ru.jamsys.http.HttpClient;
import ru.jamsys.http.HttpClientNewImpl;
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
    private String securityAlias;

    @Setter
    private String url;

    @Setter
    private String applicationName;

    private final String[] messagingScope;

    private final Security security;

    public NotificationAndroid(Security security, PropertiesManager propertiesManager) {

        this.security = security;

        this.url = propertiesManager.getProperties("rj.notification.android.url", String.class);
        this.messagingScope = new String[]{propertiesManager.getProperties("rj.notification.android.messaging.scope", String.class)};
        this.securityAlias = propertiesManager.getProperties("rj.notification.android.security.alias", String.class);
        this.storageCredentials = propertiesManager.getProperties("rj.notification.android.storage.credentials", String.class);
        this.applicationName = propertiesManager.getProperties("rj.notification.android.application.name", String.class);

    }

    @Override
    public HttpClient notify(String title, Map<String, Object> data) throws Exception {
        HttpClient httpClient = new HttpClientNewImpl();
        httpClient.setUrl(url);
        httpClient.setRequestHeader("Content-type", "application/json");
        httpClient.setRequestHeader("Authorization", "Bearer " + getAccessToken());

        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> message = new LinkedHashMap<>();
        Map<String, Object> notification = new LinkedHashMap<>();


        message.put("token", new String(security.get(securityAlias)));

        notification.put("title", applicationName);
        notification.put("body", title);
        message.put("notification", notification);

        if (!data.isEmpty()) {
            Map<String, Object> dataS = new LinkedHashMap<>();
            dataS.put("message", UtilJson.toString(data, "{}"));
            message.put("data", dataS);
        }

        root.put("message", message);

        String postData = UtilJson.toStringPretty(root, "{}");
        if (postData != null) {
            httpClient.setPostData(postData.getBytes(StandardCharsets.UTF_8));
            httpClient.exec();
        }
        return httpClient;
    }

    @Override
    public Notification getInstance() {
        Security security = App.context.getBean(Security.class);
        PropertiesManager propertiesManager = App.context.getBean(PropertiesManager.class);
        return new NotificationAndroid(security, propertiesManager);
    }

    private String getAccessToken() throws IOException {
        GoogleCredentials googleCredentials = GoogleCredentials
                .fromStream(new FileInputStream(storageCredentials))
                .createScoped(Arrays.asList(messagingScope));
        googleCredentials.refresh();
        return googleCredentials.getAccessToken().getTokenValue();
    }

}
