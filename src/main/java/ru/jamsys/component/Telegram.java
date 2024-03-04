package ru.jamsys.component;

import lombok.Setter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import ru.jamsys.http.JsonHttpResponse;
import ru.jamsys.util.JsonEnvelope;
import ru.jamsys.util.UtilJson;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@Lazy
public class Telegram extends AbstractComponent {

    private final Security security;

    @Setter
    private String securityAlias;

    public Telegram(ApplicationContext applicationContext, Security security, PropertiesManager propertiesManager) {
        super(applicationContext);
        this.security = security;
        this.securityAlias = propertiesManager.getProperties("rj.telegram.security.alias", String.class);
    }

    @SuppressWarnings("unused")
    public JsonHttpResponse syncSend(String idChat, String data) {
        return syncSend(idChat, data, null);
    }

    public JsonHttpResponse syncSend(String idChat, String data, @Nullable JsonHttpResponse refJRet) {
        JsonHttpResponse jRet = refJRet != null ? refJRet : new JsonHttpResponse();
        try {
            char[] telegramToken = security.get(securityAlias);
            if (telegramToken != null) {
                return syncSend(idChat, data, new String(telegramToken), jRet);
            } else {
                jRet.addException("Нет токена в хранилище");
            }
        } catch (Exception e) {
            jRet.addException(e);
        }
        return jRet;
    }

    public JsonHttpResponse syncSend(String idChat, String data, String apiToken, @Nullable JsonHttpResponse refJRet) {
        JsonHttpResponse jRet = refJRet != null ? refJRet : new JsonHttpResponse();
        if (jRet.isStatus() && apiToken == null) {
            jRet.addException("Telegram bot token is null");
        }
        if (jRet.isStatus() && idChat == null) {
            jRet.addException("idChatTelegram is null");
        }
        if (jRet.isStatus()) {
            try {
                String urlString = "https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s";
                urlString = String.format(urlString, apiToken, idChat, URLEncoder.encode(data, StandardCharsets.UTF_8.toString()));

                URL url = new URL(urlString);
                URLConnection conn = url.openConnection();
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);

                StringBuilder sb = new StringBuilder();
                InputStream is = new BufferedInputStream(conn.getInputStream());
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String inputLine;
                while ((inputLine = br.readLine()) != null) {
                    sb.append(inputLine);
                }
                JsonEnvelope<Map<Object, Object>> mapWrapJsonToObject = UtilJson.toMap(sb.toString());
                if (mapWrapJsonToObject.getException() != null) {
                    jRet.addException(mapWrapJsonToObject.getException());
                }
                if (jRet.isStatus()) {
                    jRet.addData("telegramResponse", mapWrapJsonToObject.getObject());
                }

            } catch (Exception e) {
                jRet.addException(e);
            }
        }
        return jRet;
    }

}
