package ru.jamsys.core.resource.yandex.speech;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.property.PropertiesAgent;
import ru.jamsys.core.extension.property.PropertyUpdateDelegate;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.resource.ResourceArguments;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;

import java.io.File;
import java.util.Map;
import java.util.function.Function;

@Component
@Scope("prototype")
public class YandexSpeechResource
        extends ExpirationMsMutableImpl
        implements
        Resource<YandexSpeechRequest, Void>,
        PropertyUpdateDelegate {

    YandexSpeechClient client = null;

    private PropertiesAgent propertiesAgent;

    private final YandexSpeechProperties property = new YandexSpeechProperties();

    @Override
    public void setArguments(ResourceArguments resourceArguments) throws Throwable {
        ServiceProperty serviceProperty = App.get(ServiceProperty.class);
        propertiesAgent = serviceProperty.getFactory().getPropertiesAgent(this, property, resourceArguments.ns, true);
    }

    @Override
    public void onPropertyUpdate(Map<String, String> mapAlias) {
        if (property.getHost() == null || property.getPort() == null || property.getAlias() == null) {
            return;
        }
        if(client != null){
            client.shutdown();
        }
        client = new YandexSpeechClient(
                property.getHost(),
                property.getPort(),
                new String(App.get(SecurityComponent.class).get(property.getAlias()))
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
        if (propertiesAgent != null) {
            propertiesAgent.run();
        }
    }

    @Override
    public void shutdown() {
        if (propertiesAgent != null) {
            propertiesAgent.shutdown();
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

}
