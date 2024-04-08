package ru.jamsys.notification;

import org.junit.jupiter.api.BeforeAll;
import ru.jamsys.App;
import ru.jamsys.http.JsonHttpResponse;

class NotificationTelegramTest {

    @BeforeAll
    static void beforeAll() {
        String[] args = new String[]{};
        App.main(args);
    }

    void send() {
        NotificationTelegram notificationTelegram = App.context.getBean(NotificationTelegram.class);
        JsonHttpResponse notify = notificationTelegram.notify("Hello", "world");
        System.out.println(notify);
    }

}