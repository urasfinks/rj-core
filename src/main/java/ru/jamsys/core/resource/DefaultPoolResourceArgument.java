package ru.jamsys.core.resource;

import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.sub.PoolSettings;
import ru.jamsys.core.resource.http.HttpResource;
import ru.jamsys.core.resource.notification.apple.AppleNotificationResource;
import ru.jamsys.core.resource.notification.apple.AppleNotificationResourceConstructor;
import ru.jamsys.core.resource.notification.telegram.TelegramNotificationResource;
import ru.jamsys.core.resource.notification.telegram.TelegramNotificationResourceConstructor;
import ru.jamsys.core.resource.notification.yandex.speech.YandexSpeechNotificationResource;
import ru.jamsys.core.resource.notification.yandex.speech.YandexSpeechNotificationResourceConstructor;
import ru.jamsys.core.resource.influx.InfluxResource;
import ru.jamsys.core.resource.influx.InfluxResourceConstructor;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.core.resource.jdbc.JdbcResourceConstructor;

import java.util.HashMap;
import java.util.Map;

@Component
public class DefaultPoolResourceArgument<R extends Resource<?, ?, RC>, RC> {

    public Map<Class<R>, PoolSettings<R, RC>> map = new HashMap<>();

    public PoolSettings<R, RC> getMapValue(Class<R> cls) {
        return map.get(cls);
    }

    @SuppressWarnings("all")
    public DefaultPoolResourceArgument() {
        map.put((Class<R>) YandexSpeechNotificationResource.class, (PoolSettings<R, RC>) new PoolSettings<>(
                YandexSpeechNotificationResource.class,
                new YandexSpeechNotificationResourceConstructor(),
                _ -> false
        ));
        map.put((Class<R>) AppleNotificationResource.class, (PoolSettings<R, RC>) new PoolSettings<>(
                AppleNotificationResource.class,
                new AppleNotificationResourceConstructor(),
                _ -> false
        ));
        map.put((Class<R>) TelegramNotificationResource.class, (PoolSettings<R, RC>) new PoolSettings<>(
                TelegramNotificationResource.class,
                new TelegramNotificationResourceConstructor(),
                _ -> false
        ));
        map.put((Class<R>) HttpResource.class, (PoolSettings<R, RC>) new PoolSettings<>(
                HttpResource.class,
                null,
                _ -> false
        ));
        map.put((Class<R>) InfluxResource.class, (PoolSettings<R, RC>) new PoolSettings<>(
                InfluxResource.class,
                new InfluxResourceConstructor(),
                _ -> false
        ));
        map.put((Class<R>) JdbcResource.class, (PoolSettings<R, RC>) new PoolSettings<>(
                JdbcResource.class,
                new JdbcResourceConstructor(),
                e -> {
                    if (e != null) {
                        String msg = e.getMessage();
                        // Не конкурентная проверка
                        return msg.contains("закрыто")
                                || msg.contains("close")
                                || msg.contains("Connection reset")
                                || msg.contains("Ошибка ввода/вывода при отправке бэкенду");
                    }
                    return false;
                }));
    }

    @SuppressWarnings("all")
    public static <R extends Resource<?, ?, ?>> PoolSettings<R, ?> get(Class<R> cls) {
        DefaultPoolResourceArgument bean = App.context.getBean(DefaultPoolResourceArgument.class);
        return bean.getMapValue(cls);
    }

}
