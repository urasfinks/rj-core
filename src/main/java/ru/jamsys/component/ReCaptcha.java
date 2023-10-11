package ru.jamsys.component;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import ru.jamsys.AbstractCoreComponent;
import ru.jamsys.JsonHttpResponse;
import ru.jamsys.UtilJson;
import ru.jamsys.WrapJsonToObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@Lazy
public class ReCaptcha extends AbstractCoreComponent {
    /**
     * Validates Google reCAPTCHA V2 or Invisible reCAPTCHA.
     */

    private final Security security;

    @Value("${rj.core.reCaptcha.secretKey:reCaptchaSecretKey}")
    private String secretKey;

    public ReCaptcha(Security security) {
        this.security = security;
    }

    public JsonHttpResponse isValid(String captchaValue, @Nullable JsonHttpResponse refJRet) {
        JsonHttpResponse jRet = refJRet != null ? refJRet : new JsonHttpResponse();
        try {
            if (jRet.isStatus() && secretKey.isEmpty()) {
                jRet.addException("Ключ reCaptchaSecretKey не определён");
            }
            if (jRet.isStatus()) {
                char[] chars = security.get(secretKey);
                if (chars == null) {
                    jRet.addException("Приватное значение ключа reCaptchaSecretKey пустое");
                }
            }
            if (jRet.isStatus()) {
                String url = "https://www.google.com/recaptcha/api/siteverify",
                        params = "secret=" + new String(security.get(secretKey)) + "&response=" + captchaValue;

                HttpURLConnection http = (HttpURLConnection) new URL(url).openConnection();
                http.setDoOutput(true);
                http.setRequestMethod("POST");
                http.setConnectTimeout(3000);
                http.setReadTimeout(3000);
                http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                OutputStream out = http.getOutputStream();
                out.write(params.getBytes(StandardCharsets.UTF_8));
                out.flush();
                out.close();

                InputStream res = http.getInputStream();
                BufferedReader rd = new BufferedReader(new InputStreamReader(res, StandardCharsets.UTF_8));

                StringBuilder sb = new StringBuilder();
                int cp;
                while ((cp = rd.read()) != -1) {
                    sb.append((char) cp);
                }
                res.close();
                String response = sb.toString();
                WrapJsonToObject<Map<Object, Object>> mapWrapJsonToObject = UtilJson.toMap(response);
                if (mapWrapJsonToObject.getException() != null) {
                    jRet.addException(mapWrapJsonToObject.getException());
                }
                jRet.addData("reCaptchaResponse", mapWrapJsonToObject.getObject());
                Boolean success = (Boolean) mapWrapJsonToObject.getObject().get("success");
                if (success == null || !success) {
                    jRet.addException("reCaptcha не пройдена");
                }
            }
        } catch (Exception e) {
            jRet.addException(e);
        }
        return jRet;
    }

    @Override
    public void flushStatistic() {

    }
}
