package ru.jamsys.core.resource.yandex.speech;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.extension.property.PropertyListener;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.resource.ResourceArguments;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImplAbstractLifeCycle;

import java.io.File;
import java.util.function.Function;

@Component
@Scope("prototype")
public class YandexSpeechResource
        extends ExpirationMsMutableImplAbstractLifeCycle
        implements
        Resource<YandexSpeechRequest, Void>,
        PropertyListener {

    YandexSpeechClient client = null;

    private PropertyDispatcher<Object> propertyDispatcher;

    private final YandexSpeechProperty yandexSpeechProperty = new YandexSpeechProperty();

    @Override
    public void setArguments(ResourceArguments resourceArguments) throws Throwable {
        propertyDispatcher = new PropertyDispatcher<>(
                App.get(ServiceProperty.class),
                this,
                yandexSpeechProperty,
                resourceArguments.ns
        );
    }

    @Override
    public Void execute(YandexSpeechRequest arguments) {
        client.synthesize(
                arguments.getText(),
                new File(arguments.getFilePath()),
                arguments,
                () -> arguments.getAsyncPromiseTask().externalComplete(),
                (Throwable th) -> arguments.getAsyncPromiseTask().externalError(th)
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
    public Function<Throwable, Boolean> getFatalException() {
        return _ -> false;
    }

    @Override
    public void onPropertyUpdate(String key, String oldValue, String newValue) {
        if (yandexSpeechProperty.getHost() == null || yandexSpeechProperty.getPort() == null || yandexSpeechProperty.getAlias() == null) {
            return;
        }
        if (client != null) {
            client.shutdown();
        }
        client = new YandexSpeechClient(
                yandexSpeechProperty.getHost(),
                yandexSpeechProperty.getPort(),
                new String(App.get(SecurityComponent.class).get(yandexSpeechProperty.getAlias()))
        );
    }

}
