package ru.jamsys.core.resource.notification.apple;

import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.manager.ManagerVirtualFileSystem;
import ru.jamsys.core.extension.property.Property;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.extension.property.PropertyListener;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.resource.ResourceArguments;
import ru.jamsys.core.resource.http.client.HttpConnector;
import ru.jamsys.core.resource.http.client.HttpConnectorDefault;
import ru.jamsys.core.resource.http.client.HttpResponse;
import ru.jamsys.core.resource.virtual.file.system.File;
import ru.jamsys.core.resource.virtual.file.system.FileLoaderFactory;
import ru.jamsys.core.resource.virtual.file.system.view.FileViewKeyStore;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class AppleNotificationResource
        extends ExpirationMsMutableImpl
        implements
        Resource<AppleNotificationRequest, HttpResponse>,
        PropertyListener {

    private ManagerVirtualFileSystem managerVirtualFileSystem;

    private PropertyDispatcher propertyDispatcher;

    private final AppleNotificationProperty appleNotificationProperty = new AppleNotificationProperty();

    @Override
    public void setArguments(ResourceArguments resourceArguments) throws Throwable {
        managerVirtualFileSystem = App.get(ManagerVirtualFileSystem.class);
        propertyDispatcher = new PropertyDispatcher(
                App.get(ServiceProperty.class),
                this,
                appleNotificationProperty,
                resourceArguments.ns
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
                managerVirtualFileSystem.get(appleNotificationProperty.getVirtualPath()),
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
    public boolean isRun() {
        if (propertyDispatcher != null) {
            return propertyDispatcher.isRun();
        }
        return false;
    }

    @Override
    public void run() {
        if (propertyDispatcher != null) {
            propertyDispatcher.run();
        }
    }

    @Override
    public void shutdown() {
        if (propertyDispatcher != null) {
            propertyDispatcher.shutdown();
        }
    }

    @Override
    public void onPropertyUpdate(String key, String oldValue, Property property) {
        if (appleNotificationProperty.getVirtualPath() == null || appleNotificationProperty.getStorage() == null) {
            return;
        }
        managerVirtualFileSystem.add(
                new File(appleNotificationProperty.getVirtualPath(), FileLoaderFactory.fromFileSystem(appleNotificationProperty.getStorage()))
        );
    }

}
