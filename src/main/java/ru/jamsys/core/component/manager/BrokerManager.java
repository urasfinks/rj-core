package ru.jamsys.core.component.manager;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.manager.item.Broker;

@Component
public class BrokerManager extends AbstractManager<Broker<?>> {

    private final ApplicationContext applicationContext;

    public BrokerManager(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public <T> EnvelopManagerObject<Broker<T>> get(String index, Class<T> classItem) {
        return new EnvelopManagerObject<>(index, classItem, this);
    }

    @Override
    public Broker<?> build(String index, Class<?> classItem) {
        return new Broker<>(index, applicationContext, classItem);
    }

}
