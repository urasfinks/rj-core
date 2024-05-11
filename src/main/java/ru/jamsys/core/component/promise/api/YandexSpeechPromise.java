package ru.jamsys.core.component.promise.api;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.Security;
import ru.jamsys.core.component.resource.PropertiesComponent;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseTask;
import ru.jamsys.core.promise.PromiseTaskType;
import ru.jamsys.core.util.YandexSpeechClient;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Пример использования
 *
 * <p>
 * <pre> {@code
 *Promise wf = new PromiseImpl("test");
 *wf.api("sound", new YandexSpeechPromise().setup((YandexSpeechPromise yandexSpeechPromise) -> {
 *    yandexSpeechPromise.setText("Привет страна");
 *    yandexSpeechPromise.setFilePath("target/result2.wav");
 *})).run().await(10000);
 *System.out.println(wf.getLog());
 * }</pre>
 */

@Getter
@Setter
public class
YandexSpeechPromise extends AbstractPromiseApi<YandexSpeechPromise> {

    private final Map<String, Object> settings = new HashMap<>();

    private String securityAlias;

    private String text = "";

    private String host;

    private int port;

    private String filePath = "";

    private PromiseTask asyncPromiseTask;

    public YandexSpeechPromise() {

        settings.put("speed", 1.0);
        settings.put("voice", "marina");
        settings.put("role", "neutral");

        PropertiesComponent propertiesComponent = App.context.getBean(PropertiesComponent.class);
        securityAlias = propertiesComponent.getProperties("rj.yandex.speech.kit.security.alias", String.class);
        host = propertiesComponent.getProperties("rj.yandex.speech.kit.host", String.class);
        port = propertiesComponent.getProperties("rj.yandex.speech.kit.port", Integer.class);

    }

    @Override
    public Consumer<AtomicBoolean> getExecutor() {
        return this::execute;
    }

    @Override
    public void extend(Promise promise) {
        super.extend(promise);
        asyncPromiseTask = new PromiseTask(getClass().getName() + "::asyncWait", promise, PromiseTaskType.ASYNC);
        promise.append(asyncPromiseTask);
    }

    private void execute(AtomicBoolean isThreadRun) {
        if (filePath.isEmpty()) {
            throw new RuntimeException("filePath is empty");
        }
        if (text.isEmpty()) {
            throw new RuntimeException("text is empty");
        }
        try {
            YandexSpeechClient client = new YandexSpeechClient(
                    host,
                    port,
                    new String(App.context.getBean(Security.class).get(securityAlias)),
                    getPromise().getExpiryRemainingMs()

            );
            client.synthesize(
                    text,
                    new File(filePath),
                    settings,
                    () -> asyncPromiseTask.complete(),
                    (Throwable t) -> asyncPromiseTask.error(t)
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
