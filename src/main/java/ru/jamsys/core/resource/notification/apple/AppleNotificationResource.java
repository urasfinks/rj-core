package ru.jamsys.core.resource.notification.apple;

import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.manager.ManagerVirtualFileSystem;
import ru.jamsys.core.extension.property.PropertiesAgent;
import ru.jamsys.core.extension.property.PropertyUpdateDelegate;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.resource.ResourceArguments;
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
import java.util.function.Function;

@Component
public class AppleNotificationResource
        extends ExpirationMsMutableImpl
        implements
        Resource<AppleNotificationRequest, HttpResponse>,
        PropertyUpdateDelegate {

    private ManagerVirtualFileSystem managerVirtualFileSystem;

    private PropertiesAgent propertiesAgent;

    private final AppleNotificationProperties property = new AppleNotificationProperties();

    @Override
    public void setArguments(ResourceArguments resourceArguments) throws Throwable {
        ServiceProperty serviceProperty = App.get(ServiceProperty.class);
        managerVirtualFileSystem = App.get(ManagerVirtualFileSystem.class);
        propertiesAgent = serviceProperty.getFactory().getPropertiesAgent(
                this,
                property,
                resourceArguments.ns,
                true
        );
    }

    @Override
    public void onPropertyUpdate(Map<String, String> mapAlias) {
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
        httpClient.setTimeoutMs(property.getTimeoutMs());

        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> aps = new LinkedHashMap<>();
        aps.put("alert", arguments.getTitle());
        root.put("aps", aps);
        root.put("message", arguments.getData());

        String postData = UtilJson.toString(root, "{}");
        if (postData != null) {
            httpClient.setPostData(postData.getBytes(StandardCharsets.UTF_8));
        }

        httpClient.putRequestHeader("apns-push-type", property.getPushType());
        httpClient.putRequestHeader("apns-expiration", property.getExpiration());
        httpClient.putRequestHeader("apns-priority", property.getPriority());
        httpClient.putRequestHeader("apns-topic", property.getTopic());

        httpClient.setKeyStore(
                managerVirtualFileSystem.get(property.getVirtualPath()),
                FileViewKeyStore.prop.SECURITY_KEY.name(), property.getSecurityAlias(),
                FileViewKeyStore.prop.TYPE.name(), "PKCS12"
        );
        httpClient.exec();
        return httpClient.getHttpResponse();
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
    public void run() {
        if (propertiesAgent != null) {
            propertiesAgent.run();
        }
    }

    @Override
    public void shutdown() {
        if (propertiesAgent != null) {
            propertiesAgent.shutdown();
        }
    }

}
