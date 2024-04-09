package ru.jamsys.notification;

import lombok.Setter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.App;
import ru.jamsys.component.PropertiesManager;
import ru.jamsys.component.VirtualFileSystem;
import ru.jamsys.http.HttpClient;
import ru.jamsys.http.HttpClientNewImpl;
import ru.jamsys.http.HttpResponseEnvelope;
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

    @Setter
    private int connectTimeoutMs;

    @Setter
    private int readTimeout;

    public NotificationApple(VirtualFileSystem virtualFileSystem, PropertiesManager propertiesManager) {

        this.virtualFileSystem = virtualFileSystem;

        this.url = propertiesManager.getProperties("rj.notification.apple.url", String.class);
        this.storage = propertiesManager.getProperties("rj.notification.apple.storage", String.class);
        this.virtualPath = propertiesManager.getProperties("rj.notification.apple.virtual.path", String.class);
        this.securityAlias = propertiesManager.getProperties("rj.notification.apple.security.alias", String.class);
        this.topic = propertiesManager.getProperties("rj.notification.apple.topic", String.class);
        this.priority = propertiesManager.getProperties("rj.notification.apple.priority", String.class);
        this.expiration = propertiesManager.getProperties("rj.notification.apple.expiration", String.class);
        this.pushType = propertiesManager.getProperties("rj.notification.apple.pushType", String.class);
        this.connectTimeoutMs = propertiesManager.getProperties("rj.notification.apple.connectTimeoutMs", Integer.class);
        this.readTimeout = propertiesManager.getProperties("rj.notification.apple.readTimeoutMs", Integer.class);

    }

    @Override
    public HttpResponseEnvelope notify(String title, Object data, String device) {
        HttpResponseEnvelope httpResponseEnvelope = new HttpResponseEnvelope();
        if (httpResponseEnvelope.isStatus() && device == null) {
            httpResponseEnvelope.addException("device is null");
        }
        if (httpResponseEnvelope.isStatus()) {
            if (!init) {
                virtualFileSystem.add(new File(virtualPath, FileLoaderFactory.fromFileSystem(storage)));
                init = true;
            }
        }

        HttpClient httpClient = null;

        if (httpResponseEnvelope.isStatus()) {
            httpClient = new HttpClientNewImpl();
            httpClient.setUrl(url + device);
            httpClient.setConnectTimeoutMillis(connectTimeoutMs);
            httpClient.setReadTimeoutMillis(readTimeout);

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

            try {
                httpClient.setKeyStore(
                        virtualFileSystem.getItem(virtualPath),
                        FileViewKeyStore.prop.SECURITY_KEY.name(), securityAlias,
                        FileViewKeyStore.prop.TYPE.name(), "PKCS12"
                );
            } catch (Exception e) {
                httpResponseEnvelope.addException(e);
            }
        }
        if (httpResponseEnvelope.isStatus() && httpClient != null) {
            httpClient.exec();
        }
        if (httpResponseEnvelope.isStatus() && httpClient != null) {
            httpClient.getHttpResponseEnvelope(httpResponseEnvelope);
        }
        return httpResponseEnvelope;
    }

    @Override
    public NotificationApple getInstance() {
        VirtualFileSystem virtualFileSystem = App.context.getBean(VirtualFileSystem.class);
        PropertiesManager propertiesManager = App.context.getBean(PropertiesManager.class);
        return new NotificationApple(virtualFileSystem, propertiesManager);
    }

}
