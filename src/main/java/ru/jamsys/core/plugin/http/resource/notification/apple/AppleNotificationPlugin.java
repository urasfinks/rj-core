package ru.jamsys.core.plugin.http.resource.notification.apple;

import ru.jamsys.core.component.manager.ManagerConfiguration;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.resource.http.client.AbstractHttpConnector;
import ru.jamsys.core.resource.http.client.HttpResponse;
import ru.jamsys.core.resource.virtual.file.system.File;
import ru.jamsys.core.resource.virtual.file.system.FileKeyStoreSSLContext;
import ru.jamsys.core.resource.virtual.file.system.ReadFromSourceFactory;

import java.nio.charset.StandardCharsets;


public class AppleNotificationPlugin {

    public static HttpResponse execute(
            AbstractHttpConnector httpConnector,
            AppleNotificationRepositoryProperty repositoryProperty
    ) {
        httpConnector.setUrl(httpConnector.getUrl() + repositoryProperty.getDevice());
        httpConnector.setBodyRaw(UtilJson.toString(new HashMapBuilder<String, Object>()
                        .append("aps", new HashMapBuilder<>().append("alert", repositoryProperty.getTitle()))
                        .append("message", repositoryProperty.getPayload()), "{}")
                .getBytes(StandardCharsets.UTF_8)
        );

        httpConnector.addRequestHeader("apns-push-type", repositoryProperty.getPushType());
        httpConnector.addRequestHeader("apns-expiration", repositoryProperty.getExpiration());
        httpConnector.addRequestHeader("apns-priority", repositoryProperty.getPriority());
        httpConnector.addRequestHeader("apns-topic", repositoryProperty.getTopic());

        // getVirtualPath является уникальным ключом, key должен быть константой в разрезе всего
        ManagerConfiguration<FileKeyStoreSSLContext> fileKeyStoreSSLContextManagerConfiguration = ManagerConfiguration.getInstance(
                FileKeyStoreSSLContext.class,
                File.class.getName(),
                repositoryProperty.getVirtualPath(),
                fileKeyStore -> {
                    fileKeyStore.setupSecurityAlias(repositoryProperty.getSecurityAlias());
                    fileKeyStore.setupTypeKeyStorage("PKCS12");
                    fileKeyStore.setupReadFromSource(ReadFromSourceFactory.fromFileSystem(repositoryProperty.getPathStorage()));
                }
        );
        httpConnector.setFileKeyStoreSSLContext(fileKeyStoreSSLContextManagerConfiguration.get());
        return httpConnector.exec();
    }

}
