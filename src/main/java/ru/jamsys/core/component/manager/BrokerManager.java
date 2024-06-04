package ru.jamsys.core.component.manager;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.manager.item.Broker;
import ru.jamsys.core.component.manager.sub.AbstractManager;
import ru.jamsys.core.component.manager.sub.ManagerElement;
import ru.jamsys.core.extension.KeepAliveComponent;

import java.util.function.Consumer;

@Component
public class BrokerManager extends AbstractManager<Broker<?>, Consumer<?>> implements KeepAliveComponent {

    private final ApplicationContext applicationContext;

    public BrokerManager(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public <T> ManagerElement<Broker<T>, Consumer<T>> get(String index, Class<T> classItem) {
        return new ManagerElement<>(index, classItem, (AbstractManager) this, null);
    }

    public <T> ManagerElement<Broker<T>, Consumer<T>> get(String index, Class<T> classItem, Consumer<T> onDrop) {
        return new ManagerElement<>(index, classItem, (AbstractManager) this, onDrop);
    }

    @Override
    public Broker<?> build(String index, Class<?> classItem, Consumer<?> builderArgument) {
        Broker<?> broker = new Broker<>(index, applicationContext, classItem);
        if (builderArgument != null) {
            broker.setOnDrop((Consumer) builderArgument);
        }
        return broker;
    }

}
