package ru.jamsys.core.component.manager;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import ru.jamsys.core.extension.*;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.log.DataHeader;
import ru.jamsys.core.flat.util.UtilRisc;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

// Менеджер объектов, которые могут прекращать свою работу по ExpirationMsMutable.
// Если объектом не пользуются - он будет остановлен и удалён
// Не надо сохранять результаты менеджера, так как они могут быть остановлены
// Менеджер работает с ключами (key), а не с пространствами имён (ns)

@Component
public class Manager extends AbstractLifeCycle implements LifeCycleComponent, StatisticsFlushComponent {

    private final ConcurrentHashMap<
            Class<? extends AbstractManagerElement>,
            ConcurrentHashMap<
                    String,
                    AbstractManagerElement
                    >
            > map = new ConcurrentHashMap<>();

    public <R extends AbstractManagerElement> void groupAcceptByInterface(Class<R> cls, Consumer<R> consumer) {
        for (Class<? extends AbstractManagerElement> key : new ArrayList<>(map.keySet())) {
            if (key.equals(cls) || cls.isAssignableFrom(key)) {
                @SuppressWarnings("unchecked")
                Class<R> t = (Class<R>) key;
                groupAccept(t, consumer);
            }
        }
    }

    public <R extends AbstractManagerElement> void groupAccept(Class<R> cls, Consumer<R> consumer) {
        Map<String, ? extends AbstractManagerElement> innerMap = map.get(cls);
        if (innerMap != null) {
            Collection<? extends AbstractManagerElement> values = innerMap.values();
            for (AbstractManagerElement element : values) {
                @SuppressWarnings("unchecked")
                R obj = (R) element;
                consumer.accept(obj);
            }
        }
    }

    // Вы должны помнить, элемент выданный этой функцией может быть остановлен если своевременно его не использовать.
    // Если у элемента не будет вызываться active() - он удалится из менеджера. Удаление - означает остановку сбора
    // статистики по нему и в целом shutdown() элемента. Работать с остановленным элементом - так себе затея.
    // Как действовать? Получили результат get(), поработали с ним и выбросили. При новой необходимости снова get()
    // не храните ссылки на результат get()

    @Getter
    @Setter
    public static class GetResult<T> {
        boolean run = true; // Элемент запущен
        T element;
    }

    public <R extends AbstractManagerElement> GetResult<R> get(Class<R> cls, String key) {
        GetResult<R> result = new GetResult<>();
        @SuppressWarnings("unchecked")
        R mapElement = (R) map.computeIfAbsent(cls, _ -> new ConcurrentHashMap<>())
                .computeIfAbsent(key, _ -> {
                    try {
                        Constructor<?> c = cls.getConstructor(String.class);
                        result.setRun(false);
                        return (AbstractManagerElement) c.newInstance(key);
                    } catch (Throwable th) {
                        throw new ForwardException("Failed to instantiate " + cls + "(String)", th);
                    }
                });
        result.setElement(mapElement);
        return result;
    }

    public <R extends AbstractManagerElement> boolean contains(Class<R> cls, String key) {
        Map<String, AbstractManagerElement> subMap =
                map.computeIfAbsent(cls, _ -> new ConcurrentHashMap<>());
        return subMap.containsKey(key);
    }

    public <R extends AbstractManagerElement> void remove(Class<R> cls, String key) {
        ConcurrentHashMap<String, AbstractManagerElement> x = map.get(cls);
        if (x == null) {
            return;
        }
        x.remove(key);
    }

    public void helper(AtomicBoolean threadRun) {
        List<LifeCycleInterface> all = new ArrayList<>();
        UtilRisc.forEach(threadRun, map, (_, mapManager) -> {
            UtilRisc.forEach(threadRun, mapManager, (key, managerElement) -> {
                if (managerElement.isExpiredWithoutStop()) {
                    all.add(managerElement);
                    mapManager.remove(key);
                } else {
                    managerElement.helper();
                }
            });
        });
        getSortShutdown(all).forEach(LifeCycleInterface::shutdown);
    }

    @Override
    public void runOperation() {
        // Всё что надо запустится само
    }

    @Override
    public void shutdownOperation() {
        List<LifeCycleInterface> all = new ArrayList<>();
        UtilRisc.forEach(null, map, (_, mapManager) -> {
            UtilRisc.forEach(null, mapManager, (_, managerElement) -> {
                all.add(managerElement);
            });
        });
        getSortShutdown(all).forEach(LifeCycleInterface::shutdown);
        map.clear();
    }

    public List<LifeCycleInterface> getSortShutdown(List<LifeCycleInterface> list) {
        GraphTopology<LifeCycleInterface> lifeCycleInterfaceGraphTopology = new GraphTopology<>();
        for (LifeCycleInterface managerElement : list) {
            if (managerElement.getListShutdownAfter().isEmpty() && managerElement.getListShutdownBefore().isEmpty()) {
                lifeCycleInterfaceGraphTopology.add(managerElement);
            } else {
                if (!managerElement.getListShutdownAfter().isEmpty()) {
                    managerElement
                            .getListShutdownAfter()
                            .forEach(lifeCycleInterface -> lifeCycleInterfaceGraphTopology
                                    .addDependency(managerElement, lifeCycleInterface)
                            );
                }
                if (!managerElement.getListShutdownBefore().isEmpty()) {
                    managerElement
                            .getListShutdownBefore()
                            .forEach(lifeCycleInterface -> lifeCycleInterfaceGraphTopology
                                    .addDependency(lifeCycleInterface, managerElement)
                            );
                }
            }
        }
        return lifeCycleInterfaceGraphTopology.getReverseSorted();
    }

    @Override
    public List<DataHeader> flushAndGetStatistic(AtomicBoolean threadRun) {
        List<DataHeader> result = new ArrayList<>();
        UtilRisc.forEach(threadRun, map, (_, mapManager) -> {
            UtilRisc.forEach(threadRun, mapManager, (_, managerElement) -> {
                result.addAll(managerElement.flushAndGetStatistic(threadRun));
            });
        });
        return result;
    }

    @Override
    public int getInitializationIndex() {
        return 5;
    }

}
