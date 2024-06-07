package ru.jamsys.core.resource.notification.apple;

import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.PropertiesComponent;
import ru.jamsys.core.component.manager.VirtualFileSystemManager;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.resource.NamespaceResourceConstructor;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.resource.balancer.algorithm.BalancerAlgorithm;
import ru.jamsys.core.resource.http.client.HttpClient;
import ru.jamsys.core.resource.http.client.HttpClientImpl;
import ru.jamsys.core.resource.http.client.HttpResponse;
import ru.jamsys.core.resource.virtual.file.system.File;
import ru.jamsys.core.resource.virtual.file.system.FileLoaderFactory;
import ru.jamsys.core.resource.virtual.file.system.view.FileViewKeyStore;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class AppleNotificationResource
        extends ExpirationMsMutableImpl
        implements Resource<NamespaceResourceConstructor, AppleNotificationRequest, HttpResponse> {

    private String virtualPath;

    private String securityAlias;

    private String url;

    private String topic;

    private String priority;

    private String expiration;

    private String pushType;

    private VirtualFileSystemManager virtualFileSystemManager;

    private int timeoutMs;

    private String storage;

    @Override
    public void constructor(NamespaceResourceConstructor constructor) throws Throwable {
        PropertiesComponent propertiesComponent = App.context.getBean(PropertiesComponent.class);
        virtualFileSystemManager = App.context.getBean(VirtualFileSystemManager.class);

        propertiesComponent.getProperties(constructor.namespaceProperties, "notification.apple.url", String.class, s -> this.url = s);
        propertiesComponent.getProperties(constructor.namespaceProperties, "notification.apple.security.alias", String.class, s -> this.securityAlias = s);
        propertiesComponent.getProperties(constructor.namespaceProperties, "notification.apple.topic", String.class, s -> this.topic = s);
        propertiesComponent.getProperties(constructor.namespaceProperties, "notification.apple.priority", String.class, s -> this.priority = s);
        propertiesComponent.getProperties(constructor.namespaceProperties, "notification.apple.expiration", String.class, s -> this.expiration = s);
        propertiesComponent.getProperties(constructor.namespaceProperties, "notification.apple.pushType", String.class, s -> this.pushType = s);
        propertiesComponent.getProperties(constructor.namespaceProperties, "notification.apple.timeoutMs", Integer.class, integer -> this.timeoutMs = integer);

        propertiesComponent.getProperties(constructor.namespaceProperties, "notification.apple.virtual.path", String.class, s -> {
            this.virtualPath = s;
            reInitClient();
        });
        propertiesComponent.getProperties(constructor.namespaceProperties, "notification.apple.storage", String.class, s -> {
            this.storage = s;
            reInitClient();
        });

    }

    private void reInitClient() {
        if (virtualPath == null || storage == null) {
            return;
        }
        virtualFileSystemManager.add(new File(virtualPath, FileLoaderFactory.fromFileSystem(storage)));
    }

    @Override
    public HttpResponse execute(AppleNotificationRequest arguments) {

        HttpClient httpClient = new HttpClientImpl();
        httpClient.setUrl(url + arguments.getDevice());
        httpClient.setTimeoutMs(timeoutMs);

        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> aps = new LinkedHashMap<>();
        aps.put("alert", arguments.getTitle());
        root.put("aps", aps);
        root.put("message", arguments.getData());

        String postData = UtilJson.toString(root, "{}");
        if (postData != null) {
            httpClient.setPostData(postData.getBytes(StandardCharsets.UTF_8));
        }

        httpClient.setRequestHeader("apns-push-type", pushType);
        httpClient.setRequestHeader("apns-expiration", expiration);
        httpClient.setRequestHeader("apns-priority", priority);
        httpClient.setRequestHeader("apns-topic", topic);

        httpClient.setKeyStore(
            virtualFileSystemManager.get(virtualPath),
            FileViewKeyStore.prop.SECURITY_KEY.name(), securityAlias,
            FileViewKeyStore.prop.TYPE.name(), "PKCS12"
        );
        httpClient.exec();
        return httpClient.getHttpResponseEnvelope();
    }

    @Override
    public void close() {

    }

    @Override
    public int getWeight(BalancerAlgorithm balancerAlgorithm) {
        return 0;
    }

}
