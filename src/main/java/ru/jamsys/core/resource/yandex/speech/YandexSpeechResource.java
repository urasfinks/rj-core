package ru.jamsys.core.resource.yandex.speech;

import com.fasterxml.jackson.annotation.JsonValue;
import ru.jamsys.core.App;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.expiration.AbstractExpirationResource;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.extension.property.PropertyListener;
import ru.jamsys.core.promise.AbstractPromiseTask;

import java.io.File;

public class YandexSpeechResource
        extends AbstractExpirationResource
        implements
        PropertyListener {

    YandexSpeechClient client = null;

    private final String ns;

    private final PropertyDispatcher<Object> propertyDispatcher;

    private final YandexSpeechRepositoryProperty property = new YandexSpeechRepositoryProperty();

    public YandexSpeechResource(String ns) {
        propertyDispatcher = new PropertyDispatcher<>(
                this,
                property,
                getCascadeKey(ns)
        );
        this.ns = ns;
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
                    asyncPromiseTask.getPromise().setError(
                            asyncPromiseTask.getNs(),
                            new ForwardException(this, th)
                    );
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

    @JsonValue
    public Object getJsonValue() {
        return new HashMapBuilder<>()
                .append("hashCode", Integer.toHexString(hashCode()))
                .append("cls", getClass())
                .append("ns", ns)
                ;
    }

    @Override
    public void onPropertyUpdate(String key, String oldValue, String newValue) {
        if (property.getHost() == null || property.getPort() == null || property.getAlias() == null) {
            return;
        }
        if (client != null) {
            client.shutdown();
        }
        try {
            client = new YandexSpeechClient(
                    property.getHost(),
                    property.getPort(),
                    new String(App.get(SecurityComponent.class).get(property.getAlias()))
            );
        } catch (Throwable th) {
            throw new ForwardException(this, th);
        }
    }

    @Override
    public boolean checkFatalException(Throwable th) {
        return false;
    }

}
