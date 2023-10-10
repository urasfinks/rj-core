package ru.jamsys.component;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.AbstractCoreComponent;
import ru.jamsys.Util;
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

    public boolean isValid(String captchaValue) {
        try {
            if (secretKey.isEmpty()) {
                Util.printStackTrace("secretKey.isEmpty()");
                return false;
            }
            char[] chars = security.get(secretKey);
            if (chars == null) {
                return false;
            }
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
            //System.out.println(response);
            WrapJsonToObject<Map<Object, Object>> mapWrapJsonToObject = UtilJson.toMap(response);
            if (mapWrapJsonToObject.getException() != null) {
                return false;
            }
            Boolean success = (Boolean) mapWrapJsonToObject.getObject().get("success");
            if (success == null) {
                return false;
            }
            return success;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void flushStatistic() {

    }
}
