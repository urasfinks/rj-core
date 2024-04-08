package ru.jamsys.http;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.SpringApplication;
import ru.jamsys.App;
import ru.jamsys.component.Core;
import ru.jamsys.notification.NotificationIos;

import java.util.HashMap;

class HttpClientNewImplTest {

    @BeforeAll
    static void beforeAll() {
        String[] args = new String[]{};
        App.context = SpringApplication.run(App.class, args);
        App.context.getBean(Core.class).run();
    }

    @SuppressWarnings("unused")
    void realSend() {
        try {
            NotificationIos NotificationIos = App.context.getBean(NotificationIos.class);
            HttpClient helloKitty = NotificationIos.notify("Hello Kitty", new HashMap<>());
            System.out.println(helloKitty);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}