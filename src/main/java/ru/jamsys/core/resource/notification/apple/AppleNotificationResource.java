package ru.jamsys.core.resource.notification.apple;

import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.PropertyComponent;
import ru.jamsys.core.component.manager.VirtualFileSystemManager;
import ru.jamsys.core.extension.property.Subscriber;
import ru.jamsys.core.extension.property.PropertySubscriberNotify;
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
import java.util.Set;

@Component
public class AppleNotificationResource
        extends ExpirationMsMutableImpl
        implements
        Resource<NamespaceResourceConstructor, AppleNotificationRequest, HttpResponse>,
        PropertySubscriberNotify {

    private VirtualFileSystemManager virtualFileSystemManager;

    private Subscriber subscriber;

    private final AppleNotificationProperty property = new AppleNotificationProperty();

    @Override
    public void constructor(NamespaceResourceConstructor constructor) throws Throwable {
        PropertyComponent propertyComponent = App.get(PropertyComponent.class);
        virtualFileSystemManager = App.get(VirtualFileSystemManager.class);
        subscriber = propertyComponent.getSubscriber(this, property, constructor.ns);
    }

    @Override
    public void onPropertyUpdate(Set<String> updatedProp) {
        if (property.getVirtualPath() == null || property.getStorage() == null) {
            return;
        }
        virtualFileSystemManager.add(
                new File(property.getVirtualPath(), FileLoaderFactory.fromFileSystem(property.getStorage()))
        );
    }

    @Override
    public HttpResponse execute(AppleNotificationRequest arguments) {

        HttpClient httpClient = new HttpClientImpl();
        httpClient.setUrl(property.getUrl() + arguments.getDevice());
        httpClient.setTimeoutMs(Integer.parseInt(property.getTimeoutMs()));

        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> aps = new LinkedHashMap<>();
        aps.put("alert", arguments.getTitle());
        root.put("aps", aps);
        root.put("message", arguments.getData());

        String postData = UtilJson.toString(root, "{}");
        if (postData != null) {
            httpClient.setPostData(postData.getBytes(StandardCharsets.UTF_8));
        }

        httpClient.setRequestHeader("apns-push-type", property.getPushType());
        httpClient.setRequestHeader("apns-expiration", property.getExpiration());
        httpClient.setRequestHeader("apns-priority", property.getPriority());
        httpClient.setRequestHeader("apns-topic", property.getTopic());

        httpClient.setKeyStore(
                virtualFileSystemManager.get(property.getVirtualPath()),
                FileViewKeyStore.prop.SECURITY_KEY.name(), property.getSecurityAlias(),
            FileViewKeyStore.prop.TYPE.name(), "PKCS12"
        );
        httpClient.exec();
        return httpClient.getHttpResponseEnvelope();
    }

    @Override
    public void close() {
        subscriber.unsubscribe();
    }

    @Override
    public int getWeight(BalancerAlgorithm balancerAlgorithm) {
        return 0;
    }

}
