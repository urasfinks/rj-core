package ru.jamsys.core.resource.notification.apple;

import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.PropComponent;
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
        PropComponent propComponent = App.context.getBean(PropComponent.class);
        virtualFileSystemManager = App.context.getBean(VirtualFileSystemManager.class);

        propComponent.getProp(constructor.ns, "notification.apple.url", s -> this.url = s);
        propComponent.getProp(constructor.ns, "notification.apple.security.alias", s -> this.securityAlias = s);
        propComponent.getProp(constructor.ns, "notification.apple.topic", s -> this.topic = s);
        propComponent.getProp(constructor.ns, "notification.apple.priority", s -> this.priority = s);
        propComponent.getProp(constructor.ns, "notification.apple.expiration", s -> this.expiration = s);
        propComponent.getProp(constructor.ns, "notification.apple.pushType", s -> this.pushType = s);
        propComponent.getProp(constructor.ns, "notification.apple.timeoutMs", s -> this.timeoutMs = Integer.parseInt(s));

        propComponent.getProp(constructor.ns, "notification.apple.virtual.path", s -> {
            this.virtualPath = s;
            reInitClient();
        });
        propComponent.getProp(constructor.ns, "notification.apple.storage", s -> {
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
