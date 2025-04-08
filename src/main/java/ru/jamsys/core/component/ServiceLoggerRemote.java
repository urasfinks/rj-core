package ru.jamsys.core.component;

import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.item.log.LogType;
import ru.jamsys.core.component.manager.item.log.PersistentData;
import ru.jamsys.core.extension.AbstractLifeCycle;
import ru.jamsys.core.extension.CascadeKey;
import ru.jamsys.core.extension.LifeCycleComponent;
import ru.jamsys.core.extension.StatisticsFlushComponent;
import ru.jamsys.core.extension.broker.persist.BrokerMemory;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.statistic.Statistic;
import ru.jamsys.core.statistic.expiration.immutable.DisposableExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

// Сервис просто пропускает через себя логи и ведёт статистику сколько по типу через него проходит

@FieldNameConstants
@Component
@Lazy
public class ServiceLoggerRemote
        extends AbstractLifeCycle
        implements
        StatisticsFlushComponent,
        LifeCycleComponent,
        CascadeKey {

    private final Map<String, AtomicInteger> stat = new HashMap<>();

    private final BrokerMemory<PersistentData> broker;

    @Getter
    private final LoggerRemoteProperty property = new LoggerRemoteProperty();

    final PropertyDispatcher<Boolean> propertyDispatcher;

    public ServiceLoggerRemote(ApplicationContext applicationContext) {
        broker = applicationContext.getBean(Core.class).getLogBroker();
        for (LogType type : LogType.values()) {
            stat.put(type.getNameCamel(), new AtomicInteger(0));
        }
        propertyDispatcher = new PropertyDispatcher<>(
                applicationContext.getBean(ServiceProperty.class),
                null,
                property,
                "log.uploader"
        );
    }

    public DisposableExpirationMsImmutableEnvelope<PersistentData> add(PersistentData persistentData) {
        stat.get(persistentData.getLogType().getNameCamel()).incrementAndGet();
        if (property.getRemote()) {
            return broker.add(new ExpirationMsImmutableEnvelope<>(persistentData, 6_000));
        }
        return null;
    }

    @Override
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean threadRun) {
        List<Statistic> result = new ArrayList<>();
        Statistic statistic = new Statistic(parentTags, parentFields);
        for (String key : stat.keySet()) {
            statistic.addField(key, stat.get(key).getAndSet(0));
        }
        result.add(statistic);
        return result;
    }

    @Override
    public int getInitializationIndex() {
        return 999;
    }

    @Override
    public void runOperation() {
        propertyDispatcher.run();
    }

    @Override
    public void shutdownOperation() {
        propertyDispatcher.shutdown();
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
