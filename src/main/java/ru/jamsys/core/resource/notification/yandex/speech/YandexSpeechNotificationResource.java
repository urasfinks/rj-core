package ru.jamsys.core.resource.notification.yandex.speech;

import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ExceptionHandler;
import ru.jamsys.core.component.PropComponent;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.flat.util.YandexSpeechClient;
import ru.jamsys.core.resource.NamespaceResourceConstructor;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.resource.balancer.algorithm.BalancerAlgorithm;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;

import java.io.File;

@Component
public class YandexSpeechNotificationResource
        extends ExpirationMsMutableImpl
        implements Resource<NamespaceResourceConstructor, YandexSpeechNotificationRequest, Void> {

    YandexSpeechClient client = null;

    String host = null;

    Integer port = null;

    String alias = null;

    @Override
    public void constructor(NamespaceResourceConstructor constructor) throws Throwable {
        PropComponent propComponent = App.context.getBean(PropComponent.class);
        propComponent.getProp(constructor.ns, "yandex.speech.kit.security.alias", String.class, s -> {
            this.alias = s;
            reInitClient();
        });
        propComponent.getProp(constructor.ns, "yandex.speech.kit.host", String.class, s -> {
            this.host = s;
            reInitClient();
        });
        propComponent.getProp(constructor.ns, "yandex.speech.kit.port", Integer.class, integer -> {
            this.port = integer;
            reInitClient();
        });
    }

    private void reInitClient() {
        if (host == null || port == null || alias == null) {
            return;
        }
        if(client != null){
            close();
        }
        client = new YandexSpeechClient(
                host,
                port,
                new String(App.context.getBean(SecurityComponent.class).get(alias))
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
            App.context.getBean(ExceptionHandler.class).handler(e);
        }
    }

    @Override
    public int getWeight(BalancerAlgorithm balancerAlgorithm) {
        return 0;
    }

}
