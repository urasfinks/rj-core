package ru.jamsys.core.component.manager;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.item.BrokerMemoryImpl;
import ru.jamsys.core.component.manager.sub.AbstractManager;
import ru.jamsys.core.extension.CascadeKey;
import ru.jamsys.core.extension.KeepAliveComponent;
import ru.jamsys.core.extension.broker.persist.BrokerMemory;

import java.util.function.Consumer;

@Component
public class ManagerBroker extends AbstractManager<BrokerMemory<?>, Consumer<?>> implements KeepAliveComponent, CascadeKey {

    private final ApplicationContext applicationContext;

    public ManagerBroker(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    // initAndGet должен вызываться раньше, чем может быть использован в других местах
    // Суть инициализации, что бы передать Consumer.onDrop, и что бы пользователей обменника оградить от понимания,
    // что будет происходить если сообщения начнуть протухать
    public <T> BrokerMemory<T> initAndGet(String key, Class<T> classItem, Consumer<T> onDrop) {
        return (BrokerMemory) getManagerElement(key, classItem, onDrop);
    }

    // Получить ранее инициализированный обменник
    public <T> BrokerMemory<T> get(String key, Class<T> classItem) {
        return (BrokerMemory) getManagerElementUnsafe(key, classItem);
    }

    @Override
    public BrokerMemory<?> build(String key, Class<?> classItem, Consumer<?> builderArgument) {
        return new BrokerMemoryImpl<>(
                getCascadeKey(key, classItem),
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
    public CascadeKey getParentCascadeKey() {
        return App.cascadeName;
    }

}
