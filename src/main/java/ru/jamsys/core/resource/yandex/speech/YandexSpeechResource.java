package ru.jamsys.core.resource.yandex.speech;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.property.Property;
import ru.jamsys.core.extension.property.PropertySubscriber;
import ru.jamsys.core.extension.property.PropertyUpdater;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.resource.ResourceArguments;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;

import java.io.File;
import java.util.function.Function;

@Component
@Scope("prototype")
public class YandexSpeechResource
        extends ExpirationMsMutableImpl
        implements
        Resource<YandexSpeechRequest, Void>,
        PropertyUpdater {

    YandexSpeechClient client = null;

    private PropertySubscriber propertySubscriber;

    private final YandexSpeechProperty yandexSpeechProperty = new YandexSpeechProperty();

    @Override
    public void setArguments(ResourceArguments resourceArguments) throws Throwable {
        propertySubscriber = new PropertySubscriber(
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
    public void run() {
        if (propertySubscriber != null) {
            propertySubscriber.run();
        }
    }

    @Override
    public void shutdown() {
        if (propertySubscriber != null) {
            propertySubscriber.shutdown();
        }
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
    public void onPropertyUpdate(String key, Property property) {
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
