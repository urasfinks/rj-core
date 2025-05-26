package ru.jamsys.core.resource.notification.apple;

import org.springframework.stereotype.Component;
import ru.jamsys.core.component.manager.ManagerConfiguration;
import ru.jamsys.core.extension.CascadeKey;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.resource.http.client.HttpConnector;
import ru.jamsys.core.resource.http.client.HttpConnectorDefault;
import ru.jamsys.core.resource.http.client.HttpResponse;
import ru.jamsys.core.resource.virtual.file.system.FileKeyStoreSSLContext;
import ru.jamsys.core.resource.virtual.file.system.ReadFromSourceFactory;
import ru.jamsys.core.extension.expiration.mutable.ExpirationMsMutableImplAbstractLifeCycle;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class AppleNotificationResource
        extends ExpirationMsMutableImplAbstractLifeCycle
        implements
        Resource<AppleNotificationRequest, HttpResponse>,
        CascadeKey {

    private PropertyDispatcher<Object> propertyDispatcher;

    private final AppleNotificationRepositoryProperty appleNotificationRepositoryProperty = new AppleNotificationRepositoryProperty();

    @Override
    public void init(String ns) throws Throwable {

        propertyDispatcher = new PropertyDispatcher<>(
                null,
                appleNotificationRepositoryProperty,
                getCascadeKey(ns)
        );
    }

    @Override
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

        ManagerConfiguration<FileKeyStoreSSLContext> fileKeyStoreSSLContextManagerConfiguration = ManagerConfiguration.getInstance(
                FileKeyStoreSSLContext.class,
                appleNotificationRepositoryProperty.getVirtualPath(),
                fileKeyStore -> {
                    fileKeyStore.setupSecurityAlias(appleNotificationRepositoryProperty.getSecurityAlias());
                    fileKeyStore.setupTypeKeyStorage("PKCS12");
                    fileKeyStore.setupReadFromSource(ReadFromSourceFactory.fromFileSystem(appleNotificationRepositoryProperty.getStorage()));
                }
        );
        httpConnector.setKeyStore(fileKeyStoreSSLContextManagerConfiguration.get());
        httpConnector.exec();
        return httpConnector.getResponseObject();
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
