package ru.jamsys.core.component.manager;

import org.springframework.stereotype.Component;
import ru.jamsys.core.component.manager.item.log.DataHeader;
import ru.jamsys.core.extension.*;
import ru.jamsys.core.flat.util.UtilRisc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

// Менеджер объектов, которые могут прекращать свою работу по ExpirationMsMutable.
// Если объектом не пользуются - он будет остановлен и удалён
// Не надо сохранять результаты менеджера, так как они могут быть остановлены
// Менеджер работает с ключами (key), а не с пространствами имён (ns)

@Component
public class Manager extends AbstractLifeCycle implements LifeCycleComponent, StatisticsFlushComponent {

    private final ConcurrentHashMap<Class<? extends ManagerElement>, ConcurrentHashMap<String, ? extends ManagerElement>> mainMap = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Function<String, ? extends ManagerElement>>> configureMap = new ConcurrentHashMap<>();

    public <R extends ManagerElement> void registerBuilder(
            Class<? extends ManagerElement> cls,
            String key,
            Function<String, R> builder
    ) {
        configureMap
                .computeIfAbsent(cls, _ -> new ConcurrentHashMap<>())
                .computeIfAbsent(key, _ -> builder);
    }

    public <R extends ManagerElement> void groupAcceptByInterface(Class<R> cls, Consumer<R> consumer) {
        for (Class<? extends ManagerElement> key : new ArrayList<>(mainMap.keySet())) {
            if (key.equals(cls) || cls.isAssignableFrom(key)) {
                @SuppressWarnings("unchecked")
                Class<R> t = (Class<R>) key;
                groupAccept(t, consumer);
            }
        }
    }

    public <R extends ManagerElement> void groupAccept(Class<R> cls, Consumer<R> consumer) {
        @SuppressWarnings("unchecked")
        Map<String, R> innerMap = (Map<String, R>) mainMap.get(cls);
        if (innerMap != null) {
            for (R element : innerMap.values()) {
                consumer.accept(element);
            }
        }
    }

    // Вы должны помнить, элемент выданный этой функцией может быть остановлен если своевременно его не использовать.
    // Если у элемента не будет вызываться active() - он удалится из менеджера. Удаление - означает остановку сбора
    // статистики по нему и в целом shutdown() элемента. Работать с остановленным элементом - так себе затея.
    // Как действовать? Получили результат get(), поработали с ним и выбросили. При новой необходимости снова get()
    // не храните ссылки на результат get()
    public <R extends ManagerElement> R get(Class<R> cls, String key) {
        @SuppressWarnings("unchecked")
        Function<String, R> builder = (Function<String, R>) configureMap.get(cls).get(key);
        return get(cls, key, builder);
    }

    public <R extends ManagerElement> R get(Class<R> cls, String key, Function<String, R> builder) {
        @SuppressWarnings("unchecked")
        Map<String, R> map = (Map<String, R>) mainMap.computeIfAbsent(cls, _ -> new ConcurrentHashMap<>());
        return map.computeIfAbsent(key, s -> {
            R apply = builder.apply(s);
            apply.run();
            return apply;
        });
    }

    @SuppressWarnings("all")
//    public <X, R extends ManagerElement> X getGeneric(Class<R> cls, String key) {
//        @SuppressWarnings("unchecked")
//        Function<String, R> builder = (Function<String, R>) configureMap.get(cls).get(key);
//        @SuppressWarnings("unchecked")
//        X x = (X) get(cls, key, builder);
//        return x;
//    }

    public <R extends ManagerElement> boolean contains(Class<R> cls, String key) {
        @SuppressWarnings("unchecked")
        Map<String, R> map = (Map<String, R>) mainMap.computeIfAbsent(cls, _ -> new ConcurrentHashMap<>());
        return map.containsKey(key);
    }

    public <R extends ManagerElement> void remove(Class<R> cls, String key) {
        configureMap.get(cls).remove(key);
        mainMap.get(cls).remove(key);
    }

    public void helper(AtomicBoolean threadRun) {
        List<LifeCycleInterface> all = new ArrayList<>();
        UtilRisc.forEach(threadRun, mainMap, (_, mapManager) -> {
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
        UtilRisc.forEach(null, mainMap, (_, mapManager) -> {
            UtilRisc.forEach(null, mapManager, (_, managerElement) -> {
                managerElement.run();
            });
        });
    }

    @Override
    public void shutdownOperation() {
        List<LifeCycleInterface> all = new ArrayList<>();
        UtilRisc.forEach(null, mainMap, (_, mapManager) -> {
            UtilRisc.forEach(null, mapManager, (_, managerElement) -> {
                all.add(managerElement);
            });
        });
        getSortShutdown(all).forEach(LifeCycleInterface::shutdown);
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
    public int getInitializationIndex() {
        return 5;
    }

    @Override
    public List<DataHeader> flushAndGetStatistic(AtomicBoolean threadRun) {
        List<DataHeader> result = new ArrayList<>();
        UtilRisc.forEach(threadRun, mainMap, (_, mapManager) -> {
            UtilRisc.forEach(threadRun, mapManager, (_, managerElement) -> {
                result.addAll(managerElement.flushAndGetStatistic(threadRun));
            });
        });
        return result;
    }

}
