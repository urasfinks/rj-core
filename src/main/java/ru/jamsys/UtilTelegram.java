package ru.jamsys;

import ru.jamsys.Util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class UtilTelegram {

    public static String syncSend(String idChat, String data) {
        if (idChat == null) {
            return "{\"status\": \"idChatTelegram is null\"}";
        }
        try {
            String urlString = "https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s";

            String apiToken = "6091094290:AAH8VKY9qyo7ezvnq_3LFw7Alh0zOS-sjqM";
            if (apiToken != null) {
                urlString = String.format(urlString, apiToken, idChat, URLEncoder.encode(data, StandardCharsets.UTF_8.toString()));

                URL url = new URL(urlString);
                URLConnection conn = url.openConnection();

                StringBuilder sb = new StringBuilder();
                InputStream is = new BufferedInputStream(conn.getInputStream());
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String inputLine = "";
                while ((inputLine = br.readLine()) != null) {
                    sb.append(inputLine);
                }
                return sb.toString();
            } else {
                return "{\"status\": \"Telegram bot token is null\"}";
            }
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> result = new HashMap<>();
            result.put("status", e.toString());
            return Util.jsonObjectToString(result);
        }
    }

}
