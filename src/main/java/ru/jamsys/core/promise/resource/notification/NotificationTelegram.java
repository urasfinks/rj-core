package ru.jamsys.core.promise.resource.notification;

import lombok.Setter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.PropertiesComponent;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.resource.http.HttpClientImpl;
import ru.jamsys.core.resource.http.HttpResponseEnvelope;
import ru.jamsys.core.flat.util.JsonEnvelope;
import ru.jamsys.core.flat.util.UtilJson;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@Lazy
public class NotificationTelegram implements Notification {

    private final SecurityComponent securityComponent;

    @Setter
    private String securityAlias;

    @Setter
    private String url;

    @Setter
    private String idChat;

    @Setter
    private int connectTimeoutMs;

    @Setter
    private int readTimeout;

    public NotificationTelegram(SecurityComponent securityComponent, PropertiesComponent propertiesComponent) {

        this.securityComponent = securityComponent;

        this.url = propertiesComponent.getProperties("default.notification.telegram.url", String.class);
        this.idChat = propertiesComponent.getProperties("default.notification.telegram.idChat", String.class);
        this.securityAlias = propertiesComponent.getProperties("default.notification.telegram.security.alias", String.class);
        this.connectTimeoutMs = propertiesComponent.getProperties("default.notification.telegram.connectTimeoutMs", Integer.class);
        this.readTimeout = propertiesComponent.getProperties("default.notification.telegram.readTimeoutMs", Integer.class);

    }

    @Override
    public HttpResponseEnvelope notify(String title, Object data, String idChat) {
        HttpResponseEnvelope httpResponseEnvelope = new HttpResponseEnvelope();
        if (httpResponseEnvelope.isStatus() && idChat == null) {
            httpResponseEnvelope.addException("idChatTelegram is null");
        }
        if (!(data instanceof String)) {
            httpResponseEnvelope.addException("data is not String type");
        }
        if (httpResponseEnvelope.isStatus()) {
            try {
                String bodyRequest = data.toString();
                if (title != null && !title.trim().isEmpty()) {
                    bodyRequest = "*" + title + "*\r\n" + bodyRequest;
                }
                String urlString = String.format(url, new String(securityComponent.get(securityAlias)), idChat, URLEncoder.encode(bodyRequest, StandardCharsets.UTF_8));

                HttpClientImpl httpClient = new HttpClientImpl();
                httpClient.setUrl(urlString);
                httpClient.setConnectTimeoutMs(connectTimeoutMs);
                httpClient.setReadTimeoutMs(readTimeout);
                httpClient.exec();

                httpClient.getHttpResponseEnvelope(httpResponseEnvelope);
                if (httpResponseEnvelope.isStatus()) {
                    JsonEnvelope<Map<Object, Object>> mapWrapJsonToObject = UtilJson.toMap(httpClient.getResponseString());
                    if (mapWrapJsonToObject.getException() != null) {
                        httpResponseEnvelope.addException(mapWrapJsonToObject.getException());
                    }
                    if (httpResponseEnvelope.isStatus()) {
                        Map<Object, Object> object = mapWrapJsonToObject.getObject();
                        if (!((boolean) object.get("ok"))) {
                            httpResponseEnvelope.addException(object.get("description").toString());
                        }
                    }
                    if (httpResponseEnvelope.isStatus()) {
                        httpResponseEnvelope.addData("telegramResponse", mapWrapJsonToObject.getObject());
                    }
                }
            } catch (Exception e) {
                httpResponseEnvelope.addException(e);
            }
        }
        return httpResponseEnvelope;
    }

    @SuppressWarnings("unused")
    public HttpResponseEnvelope notify(String title, Object data) {
        return notify(title, data, idChat);
    }

    @Override
    public Notification getInstance() {
        SecurityComponent securityComponent = App.context.getBean(SecurityComponent.class);
        PropertiesComponent propertiesComponent = App.context.getBean(PropertiesComponent.class);
        return new NotificationTelegram(securityComponent, propertiesComponent);
    }

}
