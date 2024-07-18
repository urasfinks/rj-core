package ru.jamsys.core.resource.notification.yandex.speech;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.property.PropertySubscriberNotify;
import ru.jamsys.core.extension.property.Subscriber;
import ru.jamsys.core.flat.util.YandexSpeechClient;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.resource.ResourceArguments;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;

import java.io.File;
import java.util.Set;
import java.util.function.Function;

@Component
@Scope("prototype")
public class YandexSpeechNotificationResource
        extends ExpirationMsMutableImpl
        implements
        Resource<YandexSpeechNotificationRequest, Void>,
        PropertySubscriberNotify {

    YandexSpeechClient client = null;

    private Subscriber subscriber;

    private final YandexSpeechNotificationProperty property = new YandexSpeechNotificationProperty();

    @Override
    public void setArguments(ResourceArguments resourceArguments) throws Throwable {
        ServiceProperty serviceProperty = App.get(ServiceProperty.class);
        subscriber = serviceProperty.getSubscriber(this, property, resourceArguments.ns);
    }

    @Override
    public void onPropertyUpdate(Set<String> updatedPropAlias) {
        if (property.getHost() == null || property.getPort() == null || property.getAlias() == null) {
            return;
        }
        if(client != null){
            client.shutdown();
        }
        client = new YandexSpeechClient(
                property.getHost(),
                Integer.parseInt(property.getPort()),
                new String(App.get(SecurityComponent.class).get(property.getAlias()))
        );
    }

    @Override
    public Void execute(YandexSpeechNotificationRequest arguments) {
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
    public void run() {
        if (subscriber != null) {
            subscriber.run();
        }
    }

    @Override
    public void shutdown() {
        if (subscriber != null) {
            subscriber.shutdown();
        }
        try {
            client.shutdown();
        } catch (Exception e) {
            App.error(e);
        }
    }

    @Override
    public Function<Throwable, Boolean> getFatalException() {
        return _ -> false;
    }

}
