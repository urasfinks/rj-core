package ru.jamsys.core.resource.notification.yandex.speech;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ExceptionHandler;
import ru.jamsys.core.component.PropertyComponent;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.extension.property.Subscriber;
import ru.jamsys.core.extension.property.PropertySubscriberNotify;
import ru.jamsys.core.flat.util.YandexSpeechClient;
import ru.jamsys.core.resource.NamespaceResourceConstructor;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.resource.balancer.algorithm.BalancerAlgorithm;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;

import java.io.File;
import java.util.Set;

@Component
@Scope("prototype")
public class YandexSpeechNotificationResource
        extends ExpirationMsMutableImpl
        implements Resource<NamespaceResourceConstructor, YandexSpeechNotificationRequest, Void>, PropertySubscriberNotify {

    YandexSpeechClient client = null;

    private Subscriber subscriber;

    private final YandexSpeechNotificationProperty property = new YandexSpeechNotificationProperty();

    @Override
    public void constructor(NamespaceResourceConstructor constructor) throws Throwable {
        PropertyComponent propertyComponent = App.context.getBean(PropertyComponent.class);
        subscriber = propertyComponent.getSubscriber(this, property, constructor.ns);
    }

    @Override
    public void onPropertyUpdate(Set<String> updatedProp) {
        if (property.getHost() == null || property.getPort() == null || property.getAlias() == null) {
            return;
        }
        if(client != null){
            close();
        }
        client = new YandexSpeechClient(
                property.getHost(),
                Integer.parseInt(property.getPort()),
                new String(App.context.getBean(SecurityComponent.class).get(property.getAlias()))
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
    public void close() {
        try {
            client.shutdown();
        } catch (Exception e) {
            App.error(e);
        }
        subscriber.unsubscribe();
    }

    @Override
    public int getWeight(BalancerAlgorithm balancerAlgorithm) {
        return 0;
    }

}
