package ru.jamsys.core.notification;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import ru.jamsys.core.App;
import ru.jamsys.core.component.PromiseComponent;
import ru.jamsys.core.extension.HashMapBuilder;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.resource.http.client.HttpResponse;
import ru.jamsys.core.resource.notification.android.AndroidNotificationRequest;
import ru.jamsys.core.resource.notification.android.AndroidNotificationResource;
import ru.jamsys.core.resource.notification.apple.AppleNotificationRequest;
import ru.jamsys.core.resource.notification.apple.AppleNotificationResource;
import ru.jamsys.core.resource.notification.email.EmailNotificationResource;
import ru.jamsys.core.resource.notification.email.EmailTemplateNotificationRequest;
import ru.jamsys.core.resource.notification.telegram.TelegramNotificationRequest;
import ru.jamsys.core.resource.notification.telegram.TelegramNotificationResource;
import ru.jamsys.core.resource.notification.yandex.speech.YandexSpeechNotificationRequest;
import ru.jamsys.core.resource.notification.yandex.speech.YandexSpeechNotificationResource;

import java.util.HashMap;

class NotificationTest {

    public static PromiseComponent promiseComponent;

    @BeforeAll
    static void beforeAll() {
        String[] args = new String[]{};
        App.run(args);
        promiseComponent = App.context.getBean(PromiseComponent.class);
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
    }

    @SuppressWarnings("unused")
    void telegramSend() {
        Promise promise = promiseComponent.get("testPromise", 6_000L);
        promise
                .appendWithResource("http", TelegramNotificationResource.class, (isThreadRun, _, telegramNotificationResource) -> {
                    HttpResponse execute = telegramNotificationResource.execute(new TelegramNotificationRequest("Привет", "Мир"));
                    System.out.println(execute);
                })
                .run()
                .await(2000);
        System.out.println(promise.getLog());
    }

    @SuppressWarnings("unused")
    void appleSend() {
        Promise promise = promiseComponent.get("testPromise", 6_000L);
        promise
                .appendWithResource("http", AppleNotificationResource.class, (_, _, appleNotificationResource) -> {
                    HashMapBuilder<String, Object> data = new HashMapBuilder<String, Object>().append("x1", 123);
                    HttpResponse execute = appleNotificationResource.execute(new AppleNotificationRequest("Привет", data, "e81156eeb16246fd0498c53f55f870dfc5892806dde0a6e073cbf586e761382c"));
                    System.out.println(execute);
                })
                .run()
                .await(2000);
        System.out.println(promise.getLog());
    }

    @SuppressWarnings("unused")
    void yandexTest() {
        Promise promise = promiseComponent.get("testPromise", 6_000L);
        promise
                .appendWithResource("synthesize", YandexSpeechNotificationResource.class, (_, p1, yandexSpeechNotificationResource) -> {
                    yandexSpeechNotificationResource.execute(new YandexSpeechNotificationRequest(
                            p1,
                            "target/result3.wav",
                            "Саня всё в порядке, всё в порядке Саня!"
                    ));
                })
                .run()
                .await(3000);
        System.out.println(promise.getLog());
        Assertions.assertFalse(promise.isException());
        System.out.println(promise.getLog());
    }

    @SuppressWarnings("unused")
    void androidTest() {
        Promise promise = promiseComponent.get("testPromise", 6_000L);
        promise
                .appendWithResource("push", AndroidNotificationResource.class, (atomicBoolean, promise1, androidNotificationResource) -> {
                    HashMap<String, Object> data = new HashMapBuilder<String, Object>().append("x1", 1);
                    androidNotificationResource.execute(new AndroidNotificationRequest("Приветики", data, "fyP9dxiISLW9OLJfsb73kT:APA91bGSXWN4hR9_OdXEi3THPTNs-RAsMjASA9_XXXMpq5yjkUQAG8CUvucSopPb9xcffQgyMG5K-yoA0p5JS3DyMVVTw618a566zQdvVS_a9Tmr_ktHlI5ZY5aQ60HjkhWWzI6AwsdB"));
                })
                .run()
                .await(3000);
        System.out.println(promise.getLog());
        Assertions.assertFalse(promise.isException());
        System.out.println(promise.getLog());
    }

    @SuppressWarnings("unused")
    void emailSend() {
        Promise promise = promiseComponent.get("testPromise", 6_000L);
        promise
                .appendWithResource("email", EmailNotificationResource.class, (_, _, emailNotificationResource) -> {
                    HashMap<String, String> data = new HashMapBuilder<String, String>().append("code", "999");
                    emailNotificationResource.execute(new EmailTemplateNotificationRequest(
                            "Title",
                            "data",
                            "template/email.html",
                            data,
                            "urasfinks@yandex.ru"
                    ));
                })
                .run()
                .await(3000);
        System.out.println(promise.getLog());
        Assertions.assertFalse(promise.isException());
        System.out.println(promise.getLog());
    }
}