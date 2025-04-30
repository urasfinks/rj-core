package ru.jamsys.core.component.manager;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.manager.item.log.DataHeader;
import ru.jamsys.core.extension.AbstractLifeCycle;
import ru.jamsys.core.extension.LifeCycleComponent;
import ru.jamsys.core.extension.StatisticsFlushComponent;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.flat.util.UtilRisc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

// Менеджер объектов, которые могут прекращать свою работу по ExpirationMsMutable.
// Если объектом не пользуются - он будет остановлен и удалён
// Не надо сохранять результаты менеджера, так как они могут быть выключены

@Component
public class Manager extends AbstractLifeCycle implements LifeCycleComponent, StatisticsFlushComponent {

    private final ConcurrentHashMap<Class<? extends ManagerElement>, ConcurrentHashMap<String, ? extends ManagerElement>> mainMap = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Function<String, ? extends ManagerElement>>> configureMap = new ConcurrentHashMap<>();

    // TODO: кеш бы добавить, что бы постоянно не перезапрашивать get
    public static class Configuration<T extends ManagerElement> {
        private final Class<T> cls;
        private final String namespace;
        private final Manager manager;

        public Configuration(Class<T> cls, String namespace, Manager manager) {
            this.cls = cls;
            this.namespace = namespace;
            this.manager = manager;
        }

        // Получить элемент преобразованные по типу дженерика
        public <X> X getGeneric() {
            @SuppressWarnings("unchecked")
            X t = (X) manager.get(cls, namespace);
            return t;
        }

        @JsonValue
        public Object jsonValue(){
            return new HashMapBuilder<String, Object>()
                    .append("hashCode", Integer.toHexString(hashCode()))
                    .append("cls", cls)
                    .append("namespace", namespace)
                    .append("reference", manager.get(cls, namespace));
        }

        public T get() {
            return manager.get(cls, namespace);
        }

    }

    public <R extends ManagerElement> Configuration<R> configure(Class<R> cls, String namespace) {
        return new Configuration<>(cls, namespace, this);
    }

    public <R extends ManagerElement> Configuration<R> configure(Class<R> cls, String namespace, Function<String, R> builder) {
        configureMap
                .computeIfAbsent(cls, _ -> new ConcurrentHashMap<>())
                .computeIfAbsent(namespace, _ -> builder);
        return new Configuration<>(cls, namespace, this);
    }

    // Вы должны помнить, элемент выданный этой функцией может быть остановлен если своевременно его не использовать.
    // Если у элемента не будет вызываться active() - он удалится из менеджера. Удаление - означает остановку сбора
    // статистики по нему и в целом shutdown() элемента. Работать с остановленным элементом - так себе затея.
    public <R extends ManagerElement> R get(Class<R> cls, String namespace) {
        @SuppressWarnings("unchecked")
        Function<String, R> builder = (Function<String, R>) configureMap.get(cls).get(namespace);
        return get(cls, namespace, builder);
    }

    public <X, R extends ManagerElement> X getGeneric(Class<R> cls, String namespace) {
        @SuppressWarnings("unchecked")
        Function<String, R> builder = (Function<String, R>) configureMap.get(cls).get(namespace);
        @SuppressWarnings("unchecked")
        X x = (X) get(cls, namespace, builder);
        return x;
    }

    public <R extends ManagerElement> R get(Class<R> cls, String namespace, Function<String, R> builder) {
        @SuppressWarnings("unchecked")
        Map<String, R> map = (Map<String, R>) mainMap.computeIfAbsent(cls, _ -> new ConcurrentHashMap<>());
        return map.computeIfAbsent(namespace, s -> {
            R apply = builder.apply(s);
            apply.run();
            return apply;
        });
    }

    public void helper(AtomicBoolean threadRun) {
        UtilRisc.forEach(threadRun, mainMap, (_, mapManager) -> {
            UtilRisc.forEach(threadRun, mapManager, (namespace, managerElement) -> {
                if (managerElement.isExpiredWithoutStop()) {
                    managerElement.shutdown();
                    mapManager.remove(namespace);
                } else {
                    managerElement.helper();
                }
            });
        });
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
        UtilRisc.forEach(null, mainMap, (_, mapManager) -> {
            UtilRisc.forEach(null, mapManager, (_, managerElement) -> {
                managerElement.shutdown();
            });
        });
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
