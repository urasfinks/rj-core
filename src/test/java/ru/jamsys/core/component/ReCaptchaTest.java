package ru.jamsys.core.component;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.App;
import ru.jamsys.core.component.api.ReCaptcha;
import ru.jamsys.core.resource.http.HttpResponseEnvelope;


class ReCaptchaTest {

    @BeforeAll
    static void beforeAll() {
        String[] args = new String[]{};
        App.main(args);
    }

    @Test
    void isCaptchaValid() {
        ReCaptcha reCaptcha = App.context.getBean(ReCaptcha.class);
        HttpResponseEnvelope captchaValid = reCaptcha.isValid("1234");
        System.out.println(captchaValid.isStatus());
    }
}