package ru.jamsys.core.resource.notification.apple;

import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.manager.Manager;
import ru.jamsys.core.extension.CascadeKey;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.extension.property.PropertyListener;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.resource.http.client.HttpConnector;
import ru.jamsys.core.resource.http.client.HttpConnectorDefault;
import ru.jamsys.core.resource.http.client.HttpResponse;
import ru.jamsys.core.resource.virtual.file.system.File;
import ru.jamsys.core.resource.virtual.file.system.FileLoaderFactory;
import ru.jamsys.core.resource.virtual.file.system.view.FileViewKeyStore;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImplAbstractLifeCycle;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class AppleNotificationResource
        extends ExpirationMsMutableImplAbstractLifeCycle
        implements
        Resource<AppleNotificationRequest, HttpResponse>,
        CascadeKey,
        PropertyListener {

    private PropertyDispatcher<Object> propertyDispatcher;

    private final AppleNotificationProperty appleNotificationProperty = new AppleNotificationProperty();

    @Override
    public void init(String ns) throws Throwable {

        propertyDispatcher = new PropertyDispatcher<>(
                App.get(ServiceProperty.class),
                this,
                appleNotificationProperty,
                getCascadeKey(ns)
        );
    }

    @Override
    public HttpResponse execute(AppleNotificationRequest arguments) {

        HttpConnector httpConnector = new HttpConnectorDefault();
        httpConnector.setUrl(appleNotificationProperty.getUrl() + arguments.getDevice());
        httpConnector.setConnectTimeoutMs(1_000);
        httpConnector.setReadTimeoutMs(appleNotificationProperty.getTimeoutMs());

        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> aps = new LinkedHashMap<>();
        aps.put("alert", arguments.getTitle());
        root.put("aps", aps);
        root.put("message", arguments.getData());

        String postData = UtilJson.toString(root, "{}");
        if (postData != null) {
            httpConnector.setPostData(postData.getBytes(StandardCharsets.UTF_8));
        }

        httpConnector.setRequestHeader("apns-push-type", appleNotificationProperty.getPushType());
        httpConnector.setRequestHeader("apns-expiration", appleNotificationProperty.getExpiration());
        httpConnector.setRequestHeader("apns-priority", appleNotificationProperty.getPriority());
        httpConnector.setRequestHeader("apns-topic", appleNotificationProperty.getTopic());

        httpConnector.setKeyStore(
                App.get(Manager.class).get(File.class, appleNotificationProperty.getVirtualPath()),
                FileViewKeyStore.prop.SECURITY_KEY.name(), appleNotificationProperty.getSecurityAlias(),
                FileViewKeyStore.prop.TYPE.name(), "PKCS12"
        );
        httpConnector.exec();
        return httpConnector.getResponseObject();
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Function<Throwable, Boolean> getFatalException() {
        return _ -> false;
    }

    @Override
    public void runOperation() {
        propertyDispatcher.run();
    }

    @Override
    public void shutdownOperation() {
        propertyDispatcher.shutdown();
    }

    @Override
    public void onPropertyUpdate(String key, String oldValue, String newValue) {
        if (appleNotificationProperty.getVirtualPath() == null || appleNotificationProperty.getStorage() == null) {
            return;
        }
        App.get(Manager.class).configure(
                File.class,
                appleNotificationProperty.getVirtualPath(),
                path -> new File(path, FileLoaderFactory.fromFileSystem(appleNotificationProperty.getStorage()))
        );
    }

}
