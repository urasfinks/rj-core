package ru.jamsys.core.resource.http.notification.yandex.speech;

import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.PropertiesComponent;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.extension.Completable;
import ru.jamsys.core.flat.util.YandexSpeechClient;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.resource.balancer.algorithm.BalancerAlgorithm;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;

import java.io.File;

@Component
public class YandexSpeechNotificationResource extends ExpirationMsMutableImpl implements Completable, Resource<YandexSpeechNotificationResourceConstructor, YandexSpeechNotificationRequest, Void> {

    YandexSpeechClient client = null;

    @Override
    public void constructor(YandexSpeechNotificationResourceConstructor constructor) throws Throwable {
        PropertiesComponent propertiesComponent = App.context.getBean(PropertiesComponent.class);
        String securityAlias = propertiesComponent.getProperties(constructor.namespaceProperties, "yandex.speech.kit.security.alias", String.class);
        client = new YandexSpeechClient(
                propertiesComponent.getProperties(constructor.namespaceProperties, "yandex.speech.kit.host", String.class),
                propertiesComponent.getProperties(constructor.namespaceProperties, "yandex.speech.kit.port", Integer.class),
            new String(App.context.getBean(SecurityComponent.class).get(securityAlias))
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
        client.shutdown();
    }

    @Override
    public int getWeight(BalancerAlgorithm balancerAlgorithm) {
        return 0;
    }

}
