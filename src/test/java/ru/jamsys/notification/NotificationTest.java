package ru.jamsys.notification;

import org.junit.jupiter.api.BeforeAll;
import ru.jamsys.App;
import ru.jamsys.http.HttpClient;
import ru.jamsys.http.JsonHttpResponse;

import java.util.HashMap;

class NotificationTest {

    @BeforeAll
    static void beforeAll() {
        String[] args = new String[]{};
        App.main(args);
    }

    @SuppressWarnings("unused")
    void telegramSend() {
        NotificationTelegram notificationTelegram = App.context.getBean(NotificationTelegram.class);
        JsonHttpResponse notify = notificationTelegram.notify("Hello", "world");
        System.out.println(notify);
    }

    @SuppressWarnings("unused")
    void iosSend() {
        try {
            NotificationApple NotificationApple = App.context.getBean(NotificationApple.class);
            HttpClient helloKitty = NotificationApple.notify("Hello Kitty", new HashMap<>());
            System.out.println(helloKitty);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unused")
    void androidSend() {
        try {
            NotificationAndroid NotificationAndroid = App.context.getBean(NotificationAndroid.class);
            HashMap<String, Object> data = new HashMap<>();
            data.put("x1", 1);
            HttpClient helloKitty = NotificationAndroid.notify("Hello Kitty", data);
            System.out.println(helloKitty);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}