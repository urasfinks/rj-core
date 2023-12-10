package ru.jamsys.component;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import ru.jamsys.App;
import ru.jamsys.JsonHttpResponse;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;


class ReCaptchaTest {

    @BeforeAll
    static void beforeAll() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        String[] args = new String[]{};
        App.context = SpringApplication.run(App.class, args);
        Security security = App.context.getBean(Security.class);
        security.init("12345".toCharArray());
        security.add("reCaptchaSecretKey", "hello".toCharArray());
    }

    @Test
    void isCaptchaValid() {
        ReCaptcha reCaptcha = App.context.getBean(ReCaptcha.class);
        JsonHttpResponse captchaValid = reCaptcha.isValid("1234");
        System.out.println(captchaValid.isStatus());
    }
}