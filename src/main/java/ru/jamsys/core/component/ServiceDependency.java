package ru.jamsys.core.component;

import lombok.Getter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.flat.util.UtilLog;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
@Lazy
public class ServiceDependency {

    private final Map<Class<?>, ServiceStatus> serviceMap = new ConcurrentHashMap<>();

    private final ReadWriteLock statusLock = new ReentrantReadWriteLock();

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public ServiceStatus get(Class<?> cls) {
        return serviceMap.computeIfAbsent(cls, ServiceStatus::new);
    }

    public void resetAll() {
        statusLock.writeLock().lock();
        try {
            UtilLog.printInfo("Сброс всех сервисов в активное состояние");
            serviceMap.values().forEach(ServiceStatus::activate);
        } finally {
            statusLock.writeLock().unlock();
        }
    }

    @Getter
    public static class DependencyReturn {
        private final boolean isActive;
        private final String cause;
        private final List<String> dependencyChain;

        public DependencyReturn(boolean isActive, String cause, List<String> dependencyChain) {
            this.isActive = isActive;
            this.cause = cause != null ? cause : "";
            this.dependencyChain = Collections.unmodifiableList(
                    dependencyChain != null ? dependencyChain : Collections.emptyList());
        }
    }

    @Getter
    public static class ServiceStatus {
        private final Class<?> cls;
        private final AtomicBoolean isActive = new AtomicBoolean(true);
        private volatile String cause = "";
        private volatile long changedAt;
        private final Set<ServiceStatus> dependencies = ConcurrentHashMap.newKeySet();

        public ServiceStatus(Class<?> cls) {
            this.cls = cls;
            this.changedAt = System.currentTimeMillis();
        }

        public void addDependency(ServiceStatus dependency) {
            dependencies.add(dependency);
        }

        public void removeDependency(ServiceStatus dependency) {
            dependencies.remove(dependency);
        }

        public void deactivate(String cause) {
            this.cause = cause != null ? cause : "";
            this.changedAt = System.currentTimeMillis();
            isActive.set(false);
        }

        public void activate() {
            this.cause = "";
            this.changedAt = System.currentTimeMillis();
            isActive.set(true);
        }

        public DependencyReturn checkDependency() {
            return checkDependency(new HashSet<>(), new ArrayDeque<>());
        }

        private DependencyReturn checkDependency(Set<ServiceStatus> visited, Deque<String> chain) {
            if (!visited.add(this)) {
                chain.addLast(cls.getSimpleName());
                return new DependencyReturn(false,
                        "Циклическая зависимость: " + String.join(" → ", chain),
                        new ArrayList<>(chain));
            }

            chain.addLast(cls.getSimpleName());

            if (!isActive.get()) {
                String timestamp = DATE_FORMAT.format(new Date(changedAt));
                return new DependencyReturn(false,
                        String.format("Сервис '%s' неактивен (с %s): %s",
                                cls.getSimpleName(), timestamp, cause),
                        new ArrayList<>(chain));
            }

            for (ServiceStatus dep : dependencies) {
                DependencyReturn result = dep.checkDependency(visited, chain);
                if (!result.isActive()) {
                    return result;
                }
            }

            chain.removeLast();
            visited.remove(this);
            return new DependencyReturn(true, null, new ArrayList<>(chain));
        }
    }

}
