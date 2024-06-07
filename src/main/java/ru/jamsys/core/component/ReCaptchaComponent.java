package ru.jamsys.core.component;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import ru.jamsys.core.resource.http.client.HttpResponse;
import ru.jamsys.core.flat.util.JsonEnvelope;
import ru.jamsys.core.flat.util.UtilJson;


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
//TODO: переделать на httpClient
@Component
@Lazy
@Getter
@Setter
public class ReCaptchaComponent {
    /**
     * Validates Google reCAPTCHA V2 or Invisible reCAPTCHA.
     */

    private final SecurityComponent securityComponent;
    private String securityAlias;

    public ReCaptchaComponent(SecurityComponent securityComponent, PropComponent propComponent) {
        this.securityComponent = securityComponent;
        propComponent.getProp("rj.reCaptcha.security.alias", String.class, s -> this.securityAlias = s);
    }

    @SuppressWarnings("unused")
    public HttpResponse isValid(String captchaValue) {
        return isValid(captchaValue, null);
    }

    public HttpResponse isValid(String captchaValue, @Nullable HttpResponse refJRet) {
        //TODO: переписать на HttpClient
        HttpResponse httpResponse = refJRet != null ? refJRet : new HttpResponse();
        try {
            if (httpResponse.isStatus() && securityAlias.isEmpty()) {
                httpResponse.addException("Ключ reCaptchaSecretKey не определён");
            }
            if (httpResponse.isStatus()) {
                char[] chars = securityComponent.get(securityAlias);
                if (chars == null) {
                    httpResponse.addException("Приватное значение ключа reCaptchaSecretKey пустое");
                }
            }
            if (httpResponse.isStatus()) {
                String url = "https://www.google.com/recaptcha/api/siteverify",
                        params = "secret=" + new String(securityComponent.get(securityAlias)) + "&response=" + captchaValue;

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
                    httpResponse.addException(mapWrapJsonToObject.getException());
                }
                httpResponse.setBody(mapWrapJsonToObject.getObject().toString());
                Boolean success = (Boolean) mapWrapJsonToObject.getObject().get("success");
                if (success == null || !success) {
                    httpResponse.addException("reCaptcha не пройдена");
                }
            }
        } catch (Exception e) {
            httpResponse.addException(e);
        }
        return httpResponse;
    }

}
