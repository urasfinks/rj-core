package ru.jamsys.core.promise.resource.api;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.App;
import ru.jamsys.core.promise.resource.notification.NotificationTelegram;
import ru.jamsys.core.promise.PromiseTaskExecuteType;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Пример использования
 *
 * <p>
 * <pre> {@code
*        Promise wf = new PromiseImpl("test");
*        wf.api("request", new NotificationTelegramPromise().setup((NotificationTelegramPromise telegramPromise) -> {
*            telegramPromise.setTitle("Привет");
*            telegramPromise.setData("Страна");
*        })).run().await(10000);
*        System.out.println(wf.getLog());
 * }</pre>
 */

@Getter
@Setter
public class NotificationTelegramPromise extends AbstractPromiseApi<NotificationTelegramPromise> {

    public NotificationTelegramPromise() {
        promiseTaskExecuteType = PromiseTaskExecuteType.IO;
    }

    private String title = "";

    private String data = "";

    private String idChat = null;

    final private NotificationTelegram telegramClient = App.context.getBean(NotificationTelegram.class);

    @Override
    public Consumer<AtomicBoolean> getExecutor() {
        if (data.isEmpty()) {
            throw new RuntimeException("data is empty");
        }
        if (idChat == null) {
            setResult(telegramClient.notify(title, data));
        } else {
            setResult(telegramClient.notify(title, data, idChat));
        }
        return null;
    }

}
