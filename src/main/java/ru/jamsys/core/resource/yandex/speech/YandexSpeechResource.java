package ru.jamsys.core.resource.yandex.speech;

import ru.jamsys.core.App;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.expiration.AbstractExpirationResource;
import ru.jamsys.core.extension.log.DataHeader;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.extension.property.PropertyListener;
import ru.jamsys.core.promise.AbstractPromiseTask;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class YandexSpeechResource
        extends AbstractExpirationResource
        implements
        PropertyListener {

    YandexSpeechClient client = null;

    private final PropertyDispatcher<Object> propertyDispatcher;

    private final YandexSpeechRepositoryProperty yandexSpeechRepositoryProperty = new YandexSpeechRepositoryProperty();

    public YandexSpeechResource(String ns) {
        propertyDispatcher = new PropertyDispatcher<>(
                this,
                yandexSpeechRepositoryProperty,
                getCascadeKey(ns)
        );
    }

    public Void execute(YandexSpeechRequest arguments) {
        client.synthesize(
                arguments.getText(),
                new File(arguments.getFilePath()),
                arguments,
                () -> {
                    AbstractPromiseTask asyncPromiseTask = arguments.getAsyncPromiseTask();
                    asyncPromiseTask.getPromise().completePromiseTask(asyncPromiseTask);
                },
                (Throwable th) -> {
                    AbstractPromiseTask asyncPromiseTask = arguments.getAsyncPromiseTask();
                    asyncPromiseTask.getPromise().setError(asyncPromiseTask.getNs(), new ForwardException(th));
                }
        );
        return null;
    }

    @Override
    public boolean isValid() {
        return client != null;
    }

    @Override
    public void runOperation() {
        propertyDispatcher.run();
    }

    @Override
    public void shutdownOperation() {
        propertyDispatcher.shutdown();
        try {
            client.shutdown();
        } catch (Exception e) {
            App.error(e);
        }
        client = null;
    }

    @Override
    public void onPropertyUpdate(String key, String oldValue, String newValue) {
        if (yandexSpeechRepositoryProperty.getHost() == null || yandexSpeechRepositoryProperty.getPort() == null || yandexSpeechRepositoryProperty.getAlias() == null) {
            return;
        }
        if (client != null) {
            client.shutdown();
        }
        client = new YandexSpeechClient(
                yandexSpeechRepositoryProperty.getHost(),
                yandexSpeechRepositoryProperty.getPort(),
                new String(App.get(SecurityComponent.class).get(yandexSpeechRepositoryProperty.getAlias()))
        );
    }

    @Override
    public boolean checkFatalException(Throwable th) {
        return false;
    }

    @Override
    public List<DataHeader> flushAndGetStatistic(AtomicBoolean threadRun) {
        return List.of();
    }

}
