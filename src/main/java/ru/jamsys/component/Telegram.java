package ru.jamsys.component;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import ru.jamsys.AbstractCoreComponent;
import ru.jamsys.JsonHttpResponse;
import ru.jamsys.UtilJson;
import ru.jamsys.WrapJsonToObject;

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
public class Telegram extends AbstractCoreComponent {

    private final Security security;

    public Telegram(Security security) {
        this.security = security;
    }

    @Value("${rj.core.telegram.securityKey:telegramApiToken}")
    private String securityKey;

    public JsonHttpResponse syncSend(String idChat, String data, @Nullable JsonHttpResponse refJRet) {
        JsonHttpResponse jRet = refJRet != null ? refJRet : new JsonHttpResponse();
        try {
            return syncSend(idChat, data, new String(security.get(securityKey)), jRet);
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
                WrapJsonToObject<Map<Object, Object>> mapWrapJsonToObject = UtilJson.toMap(sb.toString());
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

    @Override
    public void flushStatistic() {

    }
}
