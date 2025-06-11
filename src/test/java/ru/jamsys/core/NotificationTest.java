package ru.jamsys.core;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.property.repository.RepositoryPropertyBuilder;
import ru.jamsys.core.flat.util.UtilLog;
import ru.jamsys.core.plugin.http.resource.notification.android.AndroidNotificationPlugin;
import ru.jamsys.core.plugin.http.resource.notification.android.AndroidNotificationRepositoryProperty;
import ru.jamsys.core.plugin.http.resource.notification.apple.AppleNotificationPlugin;
import ru.jamsys.core.plugin.http.resource.notification.apple.AppleNotificationRepositoryProperty;
import ru.jamsys.core.plugin.http.resource.notification.telegram.TelegramNotificationPlugin;
import ru.jamsys.core.plugin.http.resource.notification.telegram.TelegramNotificationRepositoryProperty;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.resource.http.HttpResource;
import ru.jamsys.core.resource.http.client.HttpResponse;
import ru.jamsys.core.resource.email.EmailNotificationResource;
import ru.jamsys.core.resource.email.EmailTemplateNotificationRequest;
import ru.jamsys.core.resource.yandex.speech.YandexSpeechRequest;
import ru.jamsys.core.resource.yandex.speech.YandexSpeechResource;

import java.util.HashMap;

class NotificationTest {

    public static ServicePromise servicePromise;

    @BeforeAll
    static void beforeAll() {
        App.getRunBuilder().addTestArguments().runCore();
        servicePromise = App.get(ServicePromise.class);
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
    }

    //@Test
    @SuppressWarnings("unused")
    void telegramSend() {
        Promise promise = servicePromise.get("testPromise", 6_000L);
        promise
                .appendWithResource("http", HttpResource.class, "telegram", (_, threadRun, _, httpResource) -> {
                    HttpResponse execute = TelegramNotificationPlugin.execute(
                            httpResource.prepare(),
                            new RepositoryPropertyBuilder<>(
                                    new TelegramNotificationRepositoryProperty(),
                                    httpResource.getNs()
                            )
                                    .applyServiceProperty()
                                    .apply(TelegramNotificationRepositoryProperty.Fields.message, "Hello!")
                                    .build()
                    );
                    UtilLog.printInfo(execute);
                })
                .run()
                .await(2000);
    }

    //@Test
    @SuppressWarnings("unused")
    void appleSend() {
        Promise promise = servicePromise.get("testPromise", 6_000L);
        promise
                .appendWithResource("http", HttpResource.class, "apple", (_, _, _, httpResource) -> {
                    HttpResponse execute = AppleNotificationPlugin.execute(
                            httpResource.prepare(),
                            new RepositoryPropertyBuilder<>(
                                    new AppleNotificationRepositoryProperty(),
                                    httpResource.getNs()
                            )
                                    .applyServiceProperty()
                                    .apply(AppleNotificationRepositoryProperty.Fields.device, "e81156eeb16246fd0498c53f55f870dfc5892806dde0a6e073cbf586e761382c")
                                    .apply(AppleNotificationRepositoryProperty.Fields.title, "Привет")
                                    .applyWithoutCheck(AppleNotificationRepositoryProperty.Fields.payload, new HashMapBuilder<String, Object>().append("x1", 123))
                                    .build()
                    );
                    UtilLog.printInfo(execute);
                })
                .run()
                .await(2000);
    }

    @SuppressWarnings("unused")
    void yandexTest() {
        Promise promise = servicePromise.get("testPromise", 6_000L);
        promise
                .appendWithResource(
                        "synthesize",
                        YandexSpeechResource.class, (_, _, p1, yandexSpeechResource) ->
                                yandexSpeechResource.execute(
                                        new YandexSpeechRequest(
                                                p1,
                                                "target/result3.wav",
                                                "Саня всё в порядке, всё в порядке Саня!"
                                        )
                                )
                )
                .run()
                .await(3000);
        Assertions.assertEquals(Promise.TerminalStatus.SUCCESS, promise.getTerminalStatus());
    }

    //@Test
    @SuppressWarnings("unused")
    void androidTest() {
        Promise promise = servicePromise.get("testPromise", 6_000L);
        promise
                .appendWithResource("push", HttpResource.class, "android", (_, _, promise1, httpResource) -> {
                    HttpResponse execute = AndroidNotificationPlugin.execute(
                            httpResource.prepare(),
                            new RepositoryPropertyBuilder<>(new AndroidNotificationRepositoryProperty(), httpResource.getNs())
                                    .applyServiceProperty()
                                    .apply(AndroidNotificationRepositoryProperty.Fields.title, "Hello world")
                                    .apply(AndroidNotificationRepositoryProperty.Fields.token, "fyP9dxiISLW9OLJfsb73kT:APA91bGSXWN4hR9_OdXEi3THPTNs-RAsMjASA9_XXXMpq5yjkUQAG8CUvucSopPb9xcffQgyMG5K-yoA0p5JS3DyMVVTw618a566zQdvVS_a9Tmr_ktHlI5ZY5aQ60HjkhWWzI6AwsdB")
                                    .applyWithoutCheck(AndroidNotificationRepositoryProperty.Fields.data, new HashMapBuilder<String, Object>().append("x1", 1))
                                    .build()
                    );
                    UtilLog.printInfo(execute);
                })
                .run()
                .await(3000);
        Assertions.assertEquals(Promise.TerminalStatus.SUCCESS, promise.getTerminalStatus());
    }

    @SuppressWarnings("unused")
    void emailSend() {
        Promise promise = servicePromise.get("testPromise", 6_000L);
        promise
                .appendWithResource("email", EmailNotificationResource.class, (_, _, _, emailNotificationResource) -> {
                    HashMap<String, String> data = new HashMapBuilder<String, String>()
                            .append("code", "999")
                            .append("support", emailNotificationResource.getProperty().getSupport());
                    emailNotificationResource.execute(new EmailTemplateNotificationRequest(
                            "Title",
                            "data",
                            emailNotificationResource.getProperty().getTemplateClassLoader(),
                            emailNotificationResource.getProperty().getTemplatePath(),
                            data,
                            "urasfinks@yandex.ru"
                    ));
                })
                .run()
                .await(3000);
        Assertions.assertEquals(Promise.TerminalStatus.SUCCESS, promise.getTerminalStatus());
    }
}