package ru.jamsys.component;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import ru.jamsys.http.HttpResponseEnvelope;
import ru.jamsys.util.JsonEnvelope;
import ru.jamsys.util.UtilJson;

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
@Getter
@Setter
public class ReCaptcha {
    /**
     * Validates Google reCAPTCHA V2 or Invisible reCAPTCHA.
     */

    private final Security security;
    private String securityAlias;

    public ReCaptcha(Security security, PropertiesManager propertiesManager) {
        this.security = security;
        this.securityAlias = propertiesManager.getProperties("rj.reCaptcha.security.alias", String.class);
    }

    @SuppressWarnings("unused")
    public HttpResponseEnvelope isValid(String captchaValue) {
        return isValid(captchaValue, null);
    }

    public HttpResponseEnvelope isValid(String captchaValue, @Nullable HttpResponseEnvelope refJRet) {
        HttpResponseEnvelope httpResponseEnvelope = refJRet != null ? refJRet : new HttpResponseEnvelope();
        try {
            if (httpResponseEnvelope.isStatus() && securityAlias.isEmpty()) {
                httpResponseEnvelope.addException("Ключ reCaptchaSecretKey не определён");
            }
            if (httpResponseEnvelope.isStatus()) {
                char[] chars = security.get(securityAlias);
                if (chars == null) {
                    httpResponseEnvelope.addException("Приватное значение ключа reCaptchaSecretKey пустое");
                }
            }
            if (httpResponseEnvelope.isStatus()) {
                String url = "https://www.google.com/recaptcha/api/siteverify",
                        params = "secret=" + new String(security.get(securityAlias)) + "&response=" + captchaValue;

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
                JsonEnvelope<Map<Object, Object>> mapWrapJsonToObject = UtilJson.toMap(response);
                if (mapWrapJsonToObject.getException() != null) {
                    httpResponseEnvelope.addException(mapWrapJsonToObject.getException());
                }
                httpResponseEnvelope.addData("reCaptchaResponse", mapWrapJsonToObject.getObject());
                Boolean success = (Boolean) mapWrapJsonToObject.getObject().get("success");
                if (success == null || !success) {
                    httpResponseEnvelope.addException("reCaptcha не пройдена");
                }
            }
        } catch (Exception e) {
            httpResponseEnvelope.addException(e);
        }
        return httpResponseEnvelope;
    }

}
