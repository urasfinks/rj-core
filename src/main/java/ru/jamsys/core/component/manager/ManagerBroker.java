package ru.jamsys.core.component.manager;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.item.Broker;
import ru.jamsys.core.component.manager.sub.AbstractManager;
import ru.jamsys.core.extension.CascadeName;
import ru.jamsys.core.extension.KeepAliveComponent;

import java.util.function.Consumer;

@Component
public class ManagerBroker extends AbstractManager<Broker<?>, Consumer<?>> implements KeepAliveComponent, CascadeName {

    private final ApplicationContext applicationContext;

    public ManagerBroker(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    // initAndGet должен вызываться раньше, чем может быть использован в других местах
    // Суть инициализации, что бы передать Consumer.onDrop, и что бы пользователей обменника оградить от понимания,
    // что будет происходить если сообщения начнуть протухать
    public <T> Broker<T> initAndGet(String key, Class<T> classItem, Consumer<T> onDrop) {
        return (Broker) getManagerElement(key, classItem, onDrop);
    }

    // Получить ранее инициализированный обменник
    public <T> Broker<T> get(String key, Class<T> classItem) {
        return (Broker) getManagerElementUnsafe(key, classItem);
    }

    @Override
    public Broker<?> build(String key, Class<?> classItem, Consumer<?> builderArgument) {
        return new Broker<>(
                getCascadeName(key, classItem),
                applicationContext,
                classItem,
                (Consumer) builderArgument
        );
    }

    @Override
    public int getInitializationIndex() {
        return 3;
    }

    @Override
    public String getKey() {
        return null;
    }

    @Override
    public CascadeName getParentCascadeName() {
        return App.cascadeName;
    }

}
