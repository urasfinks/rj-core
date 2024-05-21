package ru.jamsys.core.component;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.App;
import ru.jamsys.core.resource.http.HttpResponseEnvelope;


class ReCaptchaComponentTest {

    @BeforeAll
    static void beforeAll() {
        String[] args = new String[]{};
        App.main(args);
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
    }

    @Test
    void isCaptchaValid() {
        ReCaptchaComponent reCaptchaComponent = App.context.getBean(ReCaptchaComponent.class);
        HttpResponseEnvelope captchaValid = reCaptchaComponent.isValid("1234");
        System.out.println(captchaValid.isStatus());
    }

}