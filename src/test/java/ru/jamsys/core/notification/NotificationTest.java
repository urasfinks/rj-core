package ru.jamsys.core.notification;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.App;
import ru.jamsys.core.extension.HashMapBuilder;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseImpl;
import ru.jamsys.core.promise.PromiseTaskExecuteType;
import ru.jamsys.core.promise.resource.notification.Notification;
import ru.jamsys.core.promise.resource.notification.NotificationAndroid;
import ru.jamsys.core.promise.resource.notification.NotificationEmail;
import ru.jamsys.core.resource.http.client.HttpResponse;
import ru.jamsys.core.resource.http.notification.apple.AppleRequest;
import ru.jamsys.core.resource.http.notification.apple.AppleResource;
import ru.jamsys.core.resource.http.notification.telegram.TelegramRequest;
import ru.jamsys.core.resource.http.notification.telegram.TelegramResource;

import java.util.HashMap;

class NotificationTest {
    @BeforeAll
    static void beforeAll() {
        String[] args = new String[]{};
        App.main(args);
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
    }

    @SuppressWarnings("unused")
    void telegramSend() {
        Promise promise = new PromiseImpl("testPromise", 6_000L);
        promise
                .appendWithResource("http", PromiseTaskExecuteType.IO, TelegramResource.class, (isThreadRun, telegramResource) -> {
                    HttpResponse execute = telegramResource.execute(new TelegramRequest("Привет", "Мир"));
                    System.out.println(execute);
                })
                .run()
                .await(2000);
        System.out.println(promise.getLog());
    }

    @Test
    void appleSend() {
        Promise promise = new PromiseImpl("testPromise", 6_000L);
        promise
                .appendWithResource("http", PromiseTaskExecuteType.IO, AppleResource.class, (_, appleResource) -> {
                    HashMapBuilder<String, Object> data = new HashMapBuilder<String, Object>().append("x1", 123);
                    HttpResponse execute = appleResource.execute(new AppleRequest("Привет", data, "e81156eeb16246fd0498c53f55f870dfc5892806dde0a6e073cbf586e761382c"));
                    System.out.println(execute);
                })
                .run()
                .await(2000);
        System.out.println(promise.getLog());
    }


    @SuppressWarnings("unused")
    void androidSend() {
        try {
            Notification notificationAndroid = App.context.getBean(NotificationAndroid.class);
            HashMap<String, Object> data = new HashMap<>();
            data.put("x1", 1);
            HttpResponse helloKitty = notificationAndroid.notify("Hello Kitty", data, "fyP9dxiISLW9OLJfsb73kT:APA91bGSXWN4hR9_OdXEi3THPTNs-RAsMjASA9_XXXMpq5yjkUQAG8CUvucSopPb9xcffQgyMG5K-yoA0p5JS3DyMVVTw618a566zQdvVS_a9Tmr_ktHlI5ZY5aQ60HjkhWWzI6AwsdB");
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
            HttpResponse helloKitty = notificationEmail.notify(
                    "Hello Kitty",
                    notificationEmail.compileTemplate(data),
                    "urasfinks@yandex.ru");
            System.out.println(helloKitty);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}