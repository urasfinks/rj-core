package ru.jamsys.core.resource.http.notification.apple;

import lombok.Setter;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.PropertiesComponent;
import ru.jamsys.core.component.manager.VirtualFileSystemManager;
import ru.jamsys.core.extension.Completable;
import ru.jamsys.core.flat.util.UtilJson;
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
public class AppleResource extends ExpirationMsMutableImpl implements Completable, Resource<AppleResourceConstructor, AppleRequest, HttpResponse> {

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

    private VirtualFileSystemManager virtualFileSystemManager;

    @Setter
    private int timeoutMs;

    @Override
    public void constructor(AppleResourceConstructor constructor) throws Throwable {
        PropertiesComponent propertiesComponent = App.context.getBean(PropertiesComponent.class);

        this.url = propertiesComponent.getProperties(constructor.namespaceProperties, "notification.apple.url", String.class);
        this.storage = propertiesComponent.getProperties(constructor.namespaceProperties, "notification.apple.storage", String.class);
        this.virtualPath = propertiesComponent.getProperties(constructor.namespaceProperties, "notification.apple.virtual.path", String.class);
        this.securityAlias = propertiesComponent.getProperties(constructor.namespaceProperties, "notification.apple.security.alias", String.class);
        this.topic = propertiesComponent.getProperties(constructor.namespaceProperties, "notification.apple.topic", String.class);
        this.priority = propertiesComponent.getProperties(constructor.namespaceProperties, "notification.apple.priority", String.class);
        this.expiration = propertiesComponent.getProperties(constructor.namespaceProperties, "notification.apple.expiration", String.class);
        this.pushType = propertiesComponent.getProperties(constructor.namespaceProperties, "notification.apple.pushType", String.class);
        this.timeoutMs = propertiesComponent.getProperties(constructor.namespaceProperties, "notification.apple.timeoutMs", Integer.class);

        virtualFileSystemManager = App.context.getBean(VirtualFileSystemManager.class);
        virtualFileSystemManager.add(new File(virtualPath, FileLoaderFactory.fromFileSystem(storage)));

    }

    @Override
    public HttpResponse execute(AppleRequest arguments) {

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
