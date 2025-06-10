package ru.jamsys.core.resource.notification.apple;

import ru.jamsys.core.component.manager.ManagerConfiguration;
import ru.jamsys.core.extension.expiration.AbstractExpirationResource;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.resource.http.client.HttpConnectorDefault;
import ru.jamsys.core.resource.http.client.HttpResponse;
import ru.jamsys.core.resource.virtual.file.system.File;
import ru.jamsys.core.resource.virtual.file.system.FileKeyStoreSSLContext;
import ru.jamsys.core.resource.virtual.file.system.ReadFromSourceFactory;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

// TODO: это не ресурс
public class AppleNotificationResource extends AbstractExpirationResource {

    private final PropertyDispatcher<Object> propertyDispatcher;

    private final AppleNotificationRepositoryProperty property = new AppleNotificationRepositoryProperty();

    public AppleNotificationResource(String ns) {
        propertyDispatcher = new PropertyDispatcher<>(
                null,
                property,
                getCascadeKey(ns)
        );
    }

    public HttpResponse execute(AppleNotificationRequest arguments) {

        HttpConnectorDefault httpConnector = new HttpConnectorDefault();
        httpConnector.setUrl(property.getUrl() + arguments.getDevice());
        httpConnector.setConnectTimeoutMs(1_000);
        httpConnector.setReadTimeoutMs(property.getTimeoutMs());

        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> aps = new LinkedHashMap<>();
        aps.put("alert", arguments.getTitle());
        root.put("aps", aps);
        root.put("message", arguments.getData());

        String postData = UtilJson.toString(root, "{}");
        if (postData != null) {
            httpConnector.setBodyRaw(postData.getBytes(StandardCharsets.UTF_8));
        }

        httpConnector.addRequestHeader("apns-push-type", property.getPushType());
        httpConnector.addRequestHeader("apns-expiration", property.getExpiration());
        httpConnector.addRequestHeader("apns-priority", property.getPriority());
        httpConnector.addRequestHeader("apns-topic", property.getTopic());

        // getVirtualPath является уникальным ключом, key должен быть константой в разрезе всего
        ManagerConfiguration<FileKeyStoreSSLContext> fileKeyStoreSSLContextManagerConfiguration = ManagerConfiguration.getInstance(
                FileKeyStoreSSLContext.class,
                File.class.getName(),
                property.getVirtualPath(),
                fileKeyStore -> {
                    fileKeyStore.setupSecurityAlias(property.getSecurityAlias());
                    fileKeyStore.setupTypeKeyStorage("PKCS12");
                    fileKeyStore.setupReadFromSource(ReadFromSourceFactory.fromFileSystem(property.getStorage()));
                }
        );
        httpConnector.setFileKeyStoreSSLContext(fileKeyStoreSSLContextManagerConfiguration.get());
        return httpConnector.exec();

    }

    @Override
    public boolean isValid() {
        return true;
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
    public boolean checkFatalException(Throwable th) {
        return false;
    }

}
