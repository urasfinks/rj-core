package ru.jamsys.component;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import ru.jamsys.App;
import ru.jamsys.JsonHttpResponse;
import ru.jamsys.UtilFile;


class ReCaptchaTest {

    @BeforeAll
    static void beforeAll() throws Exception {
        String[] args = new String[]{};
        App.context = SpringApplication.run(App.class, args);
        Security security = App.context.getBean(Security.class);
        security.setPathStorage("unit-test.jks");
        char[] password = "12345".toCharArray();
        security.init(password);
        security.add("reCaptchaSecretKey", "hello".toCharArray(), password);
    }

    @AfterAll
    static void afterAll() {
        UtilFile.removeIfExist("unit-test.jks");
    }

    @Test
    void isCaptchaValid() {
        ReCaptcha reCaptcha = App.context.getBean(ReCaptcha.class);
        JsonHttpResponse captchaValid = reCaptcha.isValid("1234");
        System.out.println(captchaValid.isStatus());
    }
}