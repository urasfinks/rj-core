package ru.jamsys.core.component.manager;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.manager.item.Broker;
import ru.jamsys.core.component.manager.sub.AbstractManager;
import ru.jamsys.core.component.manager.sub.ManagerElement;

@Component
public class BrokerManager extends AbstractManager<Broker<?>, Void> {

    private final ApplicationContext applicationContext;

    public BrokerManager(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public <T> ManagerElement<Broker<T>, Void> get(String index, Class<T> classItem) {
        return new ManagerElement<>(index, classItem, this, null);
    }

    @Override
    public Broker<?> build(String index, Class<?> classItem, Void customArgument) {
        return new Broker<>(index, applicationContext, classItem);
    }

}
