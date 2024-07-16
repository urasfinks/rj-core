package ru.jamsys.core.resource.notification.apple;

import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.manager.ManagerVirtualFileSystem;
import ru.jamsys.core.extension.property.Subscriber;
import ru.jamsys.core.extension.property.PropertySubscriberNotify;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.resource.NamespaceResourceConstructor;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.balancer.algorithm.BalancerAlgorithm;
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
import java.util.function.Function;

@Component
public class AppleNotificationResource
        extends ExpirationMsMutableImpl
        implements
        Resource<AppleNotificationRequest, HttpResponse>,
        PropertySubscriberNotify {

    private ManagerVirtualFileSystem managerVirtualFileSystem;

    private Subscriber subscriber;

    private final AppleNotificationProperty property = new AppleNotificationProperty();

    @Override
    public void constructor(NamespaceResourceConstructor constructor) throws Throwable {
        ServiceProperty serviceProperty = App.get(ServiceProperty.class);
        managerVirtualFileSystem = App.get(ManagerVirtualFileSystem.class);
        subscriber = serviceProperty.getSubscriber(this, property, constructor.ns);
    }

    @Override
    public void onPropertyUpdate(Set<String> updatedPropAlias) {
        if (property.getVirtualPath() == null || property.getStorage() == null) {
            return;
        }
        managerVirtualFileSystem.add(
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
                managerVirtualFileSystem.get(property.getVirtualPath()),
                FileViewKeyStore.prop.SECURITY_KEY.name(), property.getSecurityAlias(),
            FileViewKeyStore.prop.TYPE.name(), "PKCS12"
        );
        httpClient.exec();
        return httpClient.getHttpResponseEnvelope();
    }

    @Override
    public int getWeight(BalancerAlgorithm balancerAlgorithm) {
        return 0;
    }

    @Override
    public Function<Throwable, Boolean> getFatalException() {
        return _ -> false;
    }

    @Override
    public void run() {
        if (subscriber != null) {
            subscriber.run();
        }
    }

    @Override
    public void shutdown() {
        if (subscriber != null) {
            subscriber.shutdown();
        }
    }

}
