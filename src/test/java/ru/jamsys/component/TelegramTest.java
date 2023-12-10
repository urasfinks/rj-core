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

class TelegramTest {

    @BeforeAll
    static void beforeAll() {
        String[] args = new String[]{};
        App.context = SpringApplication.run(App.class, args);
    }

    @Test
    void syncSend() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        Security security = App.context.getBean(Security.class);
        security.init("12345".toCharArray());
        Telegram telegram = App.context.getBean(Telegram.class);
        JsonHttpResponse jsonHttpResponse = telegram.syncSend("-983316261", "Hello world");
        System.out.println(jsonHttpResponse.toString());
    }
}