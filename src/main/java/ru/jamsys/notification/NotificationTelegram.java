package ru.jamsys.notification;

import lombok.Setter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.App;
import ru.jamsys.component.PropertiesManager;
import ru.jamsys.component.Security;
import ru.jamsys.http.HttpClientImpl;
import ru.jamsys.http.JsonHttpResponse;
import ru.jamsys.util.JsonEnvelope;
import ru.jamsys.util.UtilJson;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@Lazy
public class NotificationTelegram implements Notification2 {

    private final Security security;

    @Setter
    private String securityAlias;

    @Setter
    private String idChat;

    @Setter
    private String url;

    @Setter
    private int connectTimeoutMs;

    @Setter
    private int readTimeout;

    public NotificationTelegram(Security security, PropertiesManager propertiesManager) {

        this.security = security;

        this.url = propertiesManager.getProperties("rj.notification.telegram.url", String.class);
        this.securityAlias = propertiesManager.getProperties("rj.notification.telegram.security.alias", String.class);
        this.idChat = propertiesManager.getProperties("rj.notification.telegram.idChat", String.class);
        this.connectTimeoutMs = propertiesManager.getProperties("rj.notification.telegram.connectTimeoutMs", Integer.class);
        this.readTimeout = propertiesManager.getProperties("rj.notification.telegram.readTimeoutMs", Integer.class);

    }

    @Override
    public JsonHttpResponse notify(String title, Object data) {
        return notify(idChat, title, data);
    }

    public JsonHttpResponse notify(String idChat, String title, Object data) {
        JsonHttpResponse jRet = new JsonHttpResponse();
        if (jRet.isStatus() && idChat == null) {
            jRet.addException("idChatTelegram is null");
        }
        if (!(data instanceof String)) {
            jRet.addException("data is not String type");
        }
        if (jRet.isStatus()) {
            try {
                String bodyRequest = data.toString();
                if (title != null && !title.trim().equals("")) {
                    bodyRequest = "*" + title + "*\r\n" + bodyRequest;
                }
                String urlString = String.format(url, new String(security.get(securityAlias)), idChat, URLEncoder.encode(bodyRequest, StandardCharsets.UTF_8.toString()));

                HttpClientImpl httpClient = new HttpClientImpl();
                httpClient.setUrl(urlString);
                httpClient.setConnectTimeoutMillis(connectTimeoutMs);
                httpClient.setReadTimeoutMillis(readTimeout);
                httpClient.exec();

                if (httpClient.getException() != null) {
                    jRet.addException(httpClient.getException());
                }
                if (jRet.isStatus()) {
                    JsonEnvelope<Map<Object, Object>> mapWrapJsonToObject = UtilJson.toMap(httpClient.getResponseString());
                    if (mapWrapJsonToObject.getException() != null) {
                        jRet.addException(mapWrapJsonToObject.getException());
                    }
                    if (jRet.isStatus()) {
                        Map<Object, Object> object = mapWrapJsonToObject.getObject();
                        if (!((boolean) object.get("ok"))) {
                            jRet.addException(object.get("description").toString());
                        }
                    }
                    if (jRet.isStatus()) {
                        jRet.addData("telegramResponse", mapWrapJsonToObject.getObject());
                    }
                }
            } catch (Exception e) {
                jRet.addException(e);
            }
        }
        return jRet;
    }

    @Override
    public Notification2 getInstance() {
        Security security = App.context.getBean(Security.class);
        PropertiesManager propertiesManager = App.context.getBean(PropertiesManager.class);
        return new NotificationTelegram(security, propertiesManager);
    }

}
