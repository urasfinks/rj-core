package ru.jamsys.core.component.manager;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.manager.item.Broker;
import ru.jamsys.core.component.manager.sub.AbstractManager;
import ru.jamsys.core.extension.KeepAliveComponent;

import java.util.function.Consumer;

@Component
public class BrokerManager extends AbstractManager<Broker<?>, Consumer<?>> implements KeepAliveComponent {

    private final ApplicationContext applicationContext;

    public BrokerManager(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public <T> Broker<T> get(String index, Class<T> classItem, Consumer<T> builderArgument) {
        return getManagerElement(index, classItem, builderArgument);
    }



    @Override
    public Broker<?> build(String index, Class<?> classItem, Consumer<?> builderArgument) {
        return new Broker<>(index, applicationContext, classItem, (Consumer) builderArgument);
    }

}
