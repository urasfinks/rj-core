package ru.jamsys.component;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


class ReCaptchaTest {
    //TODO:
    @BeforeAll
    static void beforeAll() {
//        String[] args = new String[]{};
//        App.main(args);
//        Security security = App.context.getBean(Security.class);
//        security.setPathStorage("unit-test.jks");
//        char[] password = "12345".toCharArray();
//        security.init(password);
//        security.add("reCaptchaSecretKey", "hello".toCharArray(), password);
    }

    @AfterAll
    static void afterAll() {
//        UtilFile.removeIfExist("unit-test.jks");
    }

    @Test
    void isCaptchaValid() {
//        ReCaptcha reCaptcha = App.context.getBean(ReCaptcha.class);
//        JsonHttpResponse captchaValid = reCaptcha.isValid("1234");
//        System.out.println(captchaValid.isStatus());
    }
}