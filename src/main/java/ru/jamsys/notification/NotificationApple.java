package ru.jamsys.notification;

import lombok.Setter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.App;
import ru.jamsys.component.PropertiesManager;
import ru.jamsys.component.VirtualFileSystem;
import ru.jamsys.http.HttpClient;
import ru.jamsys.http.HttpClientNewImpl;
import ru.jamsys.util.UtilJson;
import ru.jamsys.virtual.file.system.File;
import ru.jamsys.virtual.file.system.FileLoaderFactory;
import ru.jamsys.virtual.file.system.view.FileViewKeyStore;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Lazy
public class NotificationApple implements Notification {

    @Setter
    private String storage;

    @Setter
    private String virtualPath;

    @Setter
    private String securityAlias;

    @Setter
    private String url;

    @Setter
    private String topic;

    @Setter
    private String priority;

    @Setter
    private String expiration;

    @Setter
    private String pushType;

    private final VirtualFileSystem virtualFileSystem;

    private boolean init = false;

    public NotificationApple(VirtualFileSystem virtualFileSystem, PropertiesManager propertiesManager) {

        this.virtualFileSystem = virtualFileSystem;

        this.url = propertiesManager.getProperties("rj.notification.ios.url", String.class);
        this.storage = propertiesManager.getProperties("rj.notification.ios.storage", String.class);
        this.virtualPath = propertiesManager.getProperties("rj.notification.ios.virtual.path", String.class);
        this.securityAlias = propertiesManager.getProperties("rj.notification.ios.security.alias", String.class);
        this.topic = propertiesManager.getProperties("rj.notification.ios.topic", String.class);
        this.priority = propertiesManager.getProperties("rj.notification.ios.priority", String.class);
        this.expiration = propertiesManager.getProperties("rj.notification.ios.expiration", String.class);
        this.pushType = propertiesManager.getProperties("rj.notification.ios.pushType", String.class);
    }

    @Override
    public NotificationApple getInstance() {
        VirtualFileSystem virtualFileSystem = App.context.getBean(VirtualFileSystem.class);
        PropertiesManager propertiesManager = App.context.getBean(PropertiesManager.class);
        return new NotificationApple(virtualFileSystem, propertiesManager);
    }

    public HttpClient notify(String title, Map<String, Object> data) throws Exception {
        if (!init) {
            virtualFileSystem.add(new File(virtualPath, FileLoaderFactory.fromFileSystem(storage)));
            init = true;
        }
        File appleCert = virtualFileSystem.getItem(virtualPath);

        HttpClient httpClient = new HttpClientNewImpl();
        httpClient.setUrl(url);


        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> aps = new LinkedHashMap<>();
        aps.put("alert", title);
        root.put("aps", aps);
        root.put("message", data);

        String postData = UtilJson.toString(root, "{}");
        if (postData != null) {
            httpClient.setPostData(postData.getBytes(StandardCharsets.UTF_8));
            httpClient.exec();
        }

        httpClient.setRequestHeader("apns-push-type", pushType);
        httpClient.setRequestHeader("apns-expiration", expiration);
        httpClient.setRequestHeader("apns-priority", priority);
        httpClient.setRequestHeader("apns-topic", topic);

        httpClient.setKeyStore(
                appleCert,
                FileViewKeyStore.prop.SECURITY_KEY.name(), securityAlias,
                FileViewKeyStore.prop.TYPE.name(), "PKCS12"
        );

        httpClient.exec();
        return httpClient;
    }

}
