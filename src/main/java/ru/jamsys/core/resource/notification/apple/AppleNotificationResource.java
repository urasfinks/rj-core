package ru.jamsys.core.resource.notification.apple;

import ru.jamsys.core.component.manager.ManagerConfiguration;
import ru.jamsys.core.extension.expiration.AbstractExpirationResource;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.resource.http.client.HttpConnector;
import ru.jamsys.core.resource.http.client.HttpConnectorDefault;
import ru.jamsys.core.resource.http.client.HttpResponse;
import ru.jamsys.core.resource.virtual.file.system.File;
import ru.jamsys.core.resource.virtual.file.system.FileKeyStoreSSLContext;
import ru.jamsys.core.resource.virtual.file.system.ReadFromSourceFactory;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class AppleNotificationResource extends AbstractExpirationResource {

    private final PropertyDispatcher<Object> propertyDispatcher;

    private final AppleNotificationRepositoryProperty appleNotificationRepositoryProperty = new AppleNotificationRepositoryProperty();

    public AppleNotificationResource(String ns) {
        propertyDispatcher = new PropertyDispatcher<>(
                null,
                appleNotificationRepositoryProperty,
                getCascadeKey(ns)
        );
    }

    public HttpResponse execute(AppleNotificationRequest arguments) {

        HttpConnector httpConnector = new HttpConnectorDefault();
        httpConnector.setUrl(appleNotificationRepositoryProperty.getUrl() + arguments.getDevice());
        httpConnector.setConnectTimeoutMs(1_000);
        httpConnector.setReadTimeoutMs(appleNotificationRepositoryProperty.getTimeoutMs());

        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> aps = new LinkedHashMap<>();
        aps.put("alert", arguments.getTitle());
        root.put("aps", aps);
        root.put("message", arguments.getData());

        String postData = UtilJson.toString(root, "{}");
        if (postData != null) {
            httpConnector.setPostData(postData.getBytes(StandardCharsets.UTF_8));
        }

        httpConnector.setRequestHeader("apns-push-type", appleNotificationRepositoryProperty.getPushType());
        httpConnector.setRequestHeader("apns-expiration", appleNotificationRepositoryProperty.getExpiration());
        httpConnector.setRequestHeader("apns-priority", appleNotificationRepositoryProperty.getPriority());
        httpConnector.setRequestHeader("apns-topic", appleNotificationRepositoryProperty.getTopic());

        // getVirtualPath является уникальным ключом, key должен быть константой в разрезе всего
        ManagerConfiguration<FileKeyStoreSSLContext> fileKeyStoreSSLContextManagerConfiguration = ManagerConfiguration.getInstance(
                FileKeyStoreSSLContext.class,
                File.class.getName(),
                appleNotificationRepositoryProperty.getVirtualPath(),
                fileKeyStore -> {
                    fileKeyStore.setupSecurityAlias(appleNotificationRepositoryProperty.getSecurityAlias());
                    fileKeyStore.setupTypeKeyStorage("PKCS12");
                    fileKeyStore.setupReadFromSource(ReadFromSourceFactory.fromFileSystem(appleNotificationRepositoryProperty.getStorage()));
                }
        );
        httpConnector.setKeyStore(fileKeyStoreSSLContextManagerConfiguration.get());
        httpConnector.exec();
        return httpConnector.getHttpResponse();
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
