package ru.jamsys.notification;

import org.junit.jupiter.api.BeforeAll;
import ru.jamsys.App;
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
        Notification notificationTelegram = App.context.getBean(NotificationTelegram.class);
        JsonHttpResponse notify = notificationTelegram.notify("Hello", "world", "290029195");
        System.out.println(notify);
    }

    @SuppressWarnings("unused")
    void appleSend() {
        try {
            Notification notificationApple = App.context.getBean(NotificationApple.class);
            HashMap<String, Object> data = new HashMap<>();
            data.put("x1", 1);
            JsonHttpResponse helloKitty = notificationApple.notify("Hello Kitty", data, "e81156eeb16246fd0498c53f55f870dfc5892806dde0a6e073cbf586e761382c");
            System.out.println(helloKitty);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @SuppressWarnings("unused")
    void androidSend() {
        try {
            Notification notificationAndroid = App.context.getBean(NotificationAndroid.class);
            HashMap<String, Object> data = new HashMap<>();
            data.put("x1", 1);
            JsonHttpResponse helloKitty = notificationAndroid.notify("Hello Kitty", data, "fyP9dxiISLW9OLJfsb73kT:APA91bGSXWN4hR9_OdXEi3THPTNs-RAsMjASA9_XXXMpq5yjkUQAG8CUvucSopPb9xcffQgyMG5K-yoA0p5JS3DyMVVTw618a566zQdvVS_a9Tmr_ktHlI5ZY5aQ60HjkhWWzI6AwsdB");
            System.out.println(helloKitty);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unused")
    void emailSend() {
        try {
            NotificationEmail notificationEmail = App.context.getBean(NotificationEmail.class);
            HashMap<String, String> data = new HashMap<>();
            data.put("code", "321");
            JsonHttpResponse helloKitty = notificationEmail.notify(
                    "Hello Kitty",
                    notificationEmail.compileTemplate(data),
                    "urasfinks@yandex.ru");
            System.out.println(helloKitty);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}